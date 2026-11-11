package com.bigcenter.lab.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.TimeUnit;

/**
 * AQS 自实现锁演示（教程: java-core/01-并发编程/06-AQS与显式锁）：
 * 独占锁只需实现 tryAcquire/tryRelease——排队/挂起/唤醒全由 AQS 的 CLH 队列托管。
 */
public class AqsMyLockDemo {
    private static final Logger log = LoggerFactory.getLogger(AqsMyLockDemo.class);

    /** 最小独占锁：state 0=未锁 1=已锁 */
    static class MyLock extends AbstractQueuedSynchronizer implements Lock {
        @Override
        protected boolean tryAcquire(int arg) {
            // CAS 抢锁：0→1；抢到后记录持有线程（可重入判断用）
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;       // false → AQS 把当前线程入队挂起（这就是"模板方法"托管的部分）
        }

        @Override
        protected boolean tryRelease(int arg) {
            setState(0);
            setExclusiveOwnerThread(null);
            return true;
        }

        @Override
        protected boolean isHeldExclusively() { return getState() == 1; }

        @Override public void lock() { acquire(1); }
        @Override public void unlock() { release(1); }
        @Override public Condition newCondition() { return new ConditionObject(); }
        @Override public boolean tryLock() { return tryAcquire(1); }
        @Override public boolean tryLock(long t, TimeUnit u) throws InterruptedException { return tryAcquire(1); }
        @Override public void lockInterruptibly() throws InterruptedException { acquireInterruptibly(1); }
    }

    public static void main(String[] args) throws Exception {
        MyLock lock = new MyLock();
        int[] counter = new int[1];
        Thread[] threads = new Thread[4];
        for (int i = 0; i < 4; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10_000; j++) {
                    lock.lock();
                    try { counter[0]++; } finally { lock.unlock(); }   // 纪律：unlock 放 finally
                }
            }, "W" + i);
            threads[i].start();
        }
        for (Thread t : threads) t.join();
        log.info("AQS 自实现锁并发计数：counter={}（期望 40000，排队/挂起由 AQS 托管）", counter[0]);
        log.info("扩展点：tryAcquire 里加 state 判断即可变可重入；共享模式（tryAcquireShared）即信号量/CountDownLatch");
    }
}
