package com.bigcenter.lab.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * CAS 与 ABA 演示（教程: java-core/01-并发编程/04-CAS与原子类）：
 * ① AtomicInteger CAS 成功路径；② ABA 问题复现（值回了原点但中间被改过）；③ AtomicStampedReference（版本号）解决。
 */
public class CasAbaDemo {
    private static final Logger log = LoggerFactory.getLogger(CasAbaDemo.class);

    public static void main(String[] args) throws Exception {
        // ① 普通 CAS：compareAndSwap 自旋重试的原子
        AtomicInteger counter = new AtomicInteger(0);
        boolean ok = counter.compareAndSet(0, 100);
        log.info("① CAS 0→100：{}，值={}", ok, counter.get());

        // ② ABA 复现：线程主判断"值还是 A 就改 B"，但期间 A→B→A 已发生
        AtomicReferenceRaw raw = new AtomicReferenceRaw("A");
        Thread abuser = new Thread(() -> {
            raw.set("B");       // 改走
            raw.set("A");       // 改回——值回原点，CAS 感知不到
            log.info("② 中间人已完成 A→B→A");
        });
        abuser.start();
        abuser.join();
        boolean abaHit = raw.cas("A", "C");      // CAS 成功——但它看到的是"回原点"的 A
        log.info("② 普通 CAS 仍成功={}（ABA 隐患：若语义依赖'从未变过'就出错）", abaHit);

        // ③ 版本号解决：AtomicStampedReference 每次变更 stamp+1，值回原点版本号已变
        AtomicStampedReference<String> stamped = new AtomicStampedReference<>("A", 0);
        stamped.compareAndSet("A", "B", 0, 1);
        stamped.compareAndSet("B", "A", 1, 2);   // A→B→A，stamp 0→2
        boolean guarded = stamped.compareAndSet("A", "C", 0, 3);   // 期望 stamp=0，实际=2
        log.info("③ 带版本号 CAS 拒绝了 ABA：guarded={}（期望旧版本 0≠当前 2）", guarded);
    }

    /** 无版本号的引用包装（演示用：真实场景用 AtomicReference） */
    static class AtomicReferenceRaw {
        private volatile String value;
        AtomicReferenceRaw(String v) { this.value = v; }
        void set(String v) { this.value = v; }
        boolean cas(String expect, String next) {
            synchronized (this) {               // 演示简化：真实 CAS 由 Unsafe 实现
                if (value.equals(expect)) { value = next; return true; }
                return false;
            }
        }
    }
}
