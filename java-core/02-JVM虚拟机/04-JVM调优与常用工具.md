# 04 · JVM 调优与常用工具

调优的三层境界：**会用工具看（jps/jstat/jmap/jstack/Arthas）→ 会定位问题（GC 日志/OOM dump）→ 会改参数（堆/收集器/元空间）**。原则先行：**95% 的"JVM 问题"其实是代码问题**（泄漏、大对象、缓存无界），调参是最后手段。

## 常用启动参数（生产模板）

```bash
java -Xms4g -Xmx4g                      # 堆初始=最大（避免运行期扩容抖动；Xms=Xmx 是惯例）
     -Xmn1536m                          # 新生代（G1 下一般不手动设）
     -XX:+UseG1GC -XX:MaxGCPauseMillis=200
     -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m   # 元空间（动态类多时防扩容/防OOM）
     -Xss512k                           # 线程栈（千级线程可调小省内存；深递归调大）
     -XX:+HeapDumpOnOutOfMemoryError    # ★ OOM 自动 dump（生产必配！）
     -XX:HeapDumpPath=/data/dump/
     -Xlog:gc*:file=/data/logs/gc.log:time
     -jar app.jar
```

**Xms=Xmx 的原因**：堆扩容触发 Full GC + 需要向 OS 要内存；固定大小让 GC 行为可预测。maxmemory 之外的元空间在**堆外**（本地内存），容器环境要注意"堆外内存也算容器配额"（stage3/04 大 Key 同款坑：容器 OOMKilled 不一定是堆）。

## 排查工具五连（本地 + 线上通用）

```bash
jps -l                 # 列出 Java 进程（拿 PID）
jstat -gcutil <pid> 1000   # 每秒打印 GC 各区占用百分比/次数/耗时  ← 观察 GC 节奏的望远镜
jmap -dump:live,format=b,file=heap.hprof <pid>    # 堆快照（MAT/JProfiler 分析）
jstack <pid> > stack.txt           # 线程快照（CPU飙高/死锁排查）
jcmd <pid> help                    # 万能诊断命令（jcmd <pid> GC.run 等）
```

## Arthas：线上诊断神器（阿里开源，必会）

传统方式 dump 文件拉回本地分析——生产可能没权限/慢。Arthas **attach 到运行中的 JVM**，交互式诊断不停服：

```bash
java -jar arthas-boot.jar        # 选择目标进程
```

| 命令 | 用途 | 场景 |
|------|------|------|
| `dashboard` | 实时面板：线程/CPU/内存/GC | 进来先看一眼 |
| `thread -n 3` | 最占 CPU 的 3 个线程+堆栈 | **CPU 飙高定位** |
| `thread -b` | 找死锁 | 锁问题一键定位 |
| `sc -d 类名` / `sm 类名 方法` | 查类/方法的详细信息 | 确认线上到底加载了哪个版本的类（jar 冲突！） |
| `jad 类名 方法` | 反编译线上代码 | **验证线上跑的代码和预期一致** |
| `watch 类名 方法 '{params, returnObj}' -x 2` | 观察方法入参/返回值 | **线上打点**，不用改代码重新发版 |
| `trace 类名 方法` | 方法内部各调用耗时 | 慢在哪一跳 |
| `heapdump /tmp/heap.hprof` | dump 堆 | 内存泄漏分析 |

`watch`/`trace` 是 Arthas 的灵魂——**线上问题的实时探针**，理论上限是"不重启解决大部分线上问题"。

## CPU 飙高排查 SOP（背下这套流程）

```
① top                    → 找到吃 CPU 的 Java 进程 PID
② top -Hp <pid>          → 找到该进程里吃 CPU 的线程 TID
③ printf '%x\n' <TID>    → 线程 ID 转 16 进制（如 0x1a2b）
④ jstack <pid> | grep 0x1a2b -A 20   → 看这个线程的堆栈在干什么
```

常见结局：死循环 / 频繁 GC（GC 线程吃 CPU → 回去查内存）/ 序列化大对象 / 正则回溯爆炸。
**Arthas 一条命令版**：`thread -n 3` 直接给出答案。

## 内存泄漏排查 SOP

```
症状：堆水位持续上涨不回落 → Full GC 后回落一点点 → 最终 OOM
① jmap -dump 拿堆快照（或 OOM 时自动 dump 的文件）
② MAT/JProfiler 打开 → Leak Suspects 报告 → 看"最大对象"被谁持有
   → 常见凶手：静态 Map 缓存无限增长 / ThreadLocal 未 remove / 监听器未注销 /
     连接未关闭 / ThreadLocal 在线程池线程上残留（并发篇07）
③ 定位代码 → 修（上限/过期/移除）
```

**判定泄漏的黄金标准**：Full GC 之后堆水位**回落不到基线**且逐次抬高。GC 后能回落就只是"用得多"不是"漏"。

## GC 日志怎么读（G1 示例）

```
[gc] GC(12) Pause Young (Normal) (G1 Evacuation Pause)  1200M->300M(4096M) 25.3ms
                     ↑ 年轻代回收        ↑ 回收前→后    总容量   ↑ 停顿耗时
```

关注四个数：**回收前后差值**（每轮收出多少垃圾）、**停顿时长**（超目标=告警）、**距上次 GC 间隔**（频率）、**Full GC 是否出现**（出现即问题）。趋势判断：GC 越来越频繁 + 回收后水位抬升 = 泄漏前兆。

## 调优实战的三个典型场景

| 场景 | 现象 | 调优动作 |
|------|------|---------|
| 大促前容量评估 | 平峰没事高峰卡 | 压测确定堆水位，Xms=Xmx 固定，预热缓存 |
| 接口偶发 200ms 尖刺 | P99 远高于 P50 | 看 GC 日志停顿点 → G1 调 MaxGCPauseMillis / 减少大对象分配 / 考虑 ZGC |
| 发布后 Metaspace 增长 | 一直涨 | 动态类生成检查（脚本引擎/CGLib）+ MaxMetaspaceSize 防爆 |

## 面试问法

**Q: 线上 CPU 100% 怎么排查？**
A: top 定位进程 → top -Hp 定位线程 → 线程 ID 转 16 进制 → jstack 对应堆栈。常见原因：死循环、频繁 GC（结合 jstat -gcutil）、正则回溯、大对象序列化。有 Arthas 时 `thread -n 3` 一步到位，`watch/trace` 进一步定位业务代码。

**Q: 内存泄漏怎么排查？**
A: 判定标准是 Full GC 后水位回落不到基线且持续抬升。jmap dump（或 OOM 自动 dump）→ MAT 看支配树/泄漏疑点 → 常见凶手：无界缓存/ThreadLocal/监听器/连接。修复方向：容量上限、TTL/弱引用、finally 清理。预防：HeapDumpOnOutOfMemoryError + 水位告警。

---

**下一篇**：[05-常见OOM与线上排查](05-常见OOM与线上排查.md)（四型 OOM 速查 + 综合实战案例）。
