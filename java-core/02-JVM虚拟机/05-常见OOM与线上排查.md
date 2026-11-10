# 05 · 常见 OOM 与线上排查综合实战

本篇是 JVM 模块的实战收官：四型 OOM 速查表 + 一个综合排查演练（从零定位到修复），全部可本地复现。

## 四型 OOM 速查表（对着异常信息直接定位）

| 异常 | 位置 | 常见原因 | 修复 |
|------|------|---------|------|
| `Java heap space` | 堆 | 对象太多（真用得多）/内存泄漏（该回收没回收）/堆太小 | dump 分析找出大对象持有链；调 -Xmx；修泄漏 |
| `GC overhead limit exceeded` | 堆 | GC 时间占比 >98% 却回收不到 2%——**泄漏的前兆形态** | 同上，尽早 dump |
| `Metaspace` | 元空间 | 动态生成类：CGLib 代理、Groovy 脚本、反射滥用、热部署 | 限制/复用生成器；调 MaxMetaspaceSize |
| `StackOverflowError` | 虚拟机栈 | 递归无出口/递归深度过大/栈帧大 | 修递归（改循环+备忘录）；-Xss 调大治标 |
| `Unable to create native thread` | 无法建线程 | 线程数超 OS 限制（ulimit）或内存不够分配线程栈 | 线程池收敛；调 ulimit；虚拟线程改造（stage6/01） |

**第一反应永远相同**：有 dump 吗？没 dump 就 `jmap -dump` 或重启前配 `-XX:+HeapDumpOnOutOfMemoryError`——**没有现场的排查都是玄学**。

## 综合实战演练：模拟"内存泄漏 + OOM"并完成排查闭环

以下代码可在本地完整复现一次"泄漏→告警→dump→定位→修复"的全流程：

```java
// 泄漏点：无界缓存（生产最常见泄漏形态）
public class LeakDemo {
    static Map<String, byte[]> cache = new HashMap<>();      // 无上限！

    public static void main(String[] args) throws Exception {
        long i = 0;
        while (true) {
            cache.put("item-" + i++, new byte[1024 * 100]);  // 每轮 100KB
            Thread.sleep(5);
            // 模拟修复后的正确版：加容量上限 + LRU 淘汰（LinkedHashMap/accessOrder）
        }
    }
}
```

```bash
java -Xms64m -Xmx64m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=. LeakDemo
# 约 30~60 秒后：OutOfMemoryError: Java heap space，heap.hprof 自动生成
```

排查动作：

```
1. MAT / IntelliJ（打开 hprof）→ Leak Suspects 报告
2. 最大对象：HashMap$Node[] 占 90%+ → 持有链指向 LeakDemo.cache
3. 结论：无界缓存泄漏 → 修复：容量上限 + 淘汰策略（或改 Caffeine maximumSize）
4. 回归：写一个 Testcontainers/单元测试锁住"缓存有上限"的行为（stage6/07）
```

## 线上排查综合案例（从告警到修复的完整故事线）

```
10:03 告警：服务 P99 延迟从 80ms 涨到 3s
10:05 登录看板：jstat -gcutil 发现 Full GC 每 30 秒一次，每次 8 秒
      → 推理：Full GC 频繁且停顿长，堆出问题了
10:07 jmap -dump 抢救现场（趁服务还没 OOM 重启）
10:10 MAT 分析：byte[] 占堆 70%，持有链 → 某报表服务把 50w 行数据全量拉进内存
10:15 修复：分页查询 + 流式处理（MyBatis Cursor / 分批），当日恢复
事后：①加上"单次查询行数上限"代码规约 ②堆水位告警 ③复盘进踩坑实录
```

**这个故事的每一步都对应前面章节的工具**：告警（Metrics）→ GC 节奏（jstat）→ 现场（jmap）→ 分析（MAT）→ 修复（代码）→ 预防（规约+监控）。**排查 = 用工具把"现象"翻译成"证据链"**。

## 三个高频线上症状速查（拓展）

| 症状 | 首查 | 常见根因 |
|------|------|---------|
| CPU 100% | jstack/Arthas thread -n 3 | 死循环、频繁 GC、正则回溯、大集合排序 |
| 接口批量超时 | GC 日志 + Trace | Full GC 停顿、下游故障、连接池耗尽 |
| 服务重启循环 | 启动日志 + OOM dump | OOM（堆/元空间）、健康检查失败、端口冲突 |

## JVM 模块收束：一张全景图

```
写代码时：内存区域（01）决定"东西放哪"；类加载（02）决定"类从哪来"
程序运行：GC（03）决定"还能不能活"；工具（04）负责"看得见"
出事故时：OOM 速查（05）+ 排查 SOP
三个对应关系（记忆锚点）：
  锁升级(并发02) ↔ Mark Word(内存01)     ——同一个字节的两副面孔
  ThreadLocal泄漏(并发07) ↔ 弱引用(GC03)  ——弱引用回收时 value 还在强链上
  binlog(教MQ时) ↔ GC日志                ——都是"事实日志"，故障回放的基础
```

## 面试终极题

**Q: 讲一次你排查线上 JVM 问题的经历。**
A: 按"现象→数据→根因→修复→预防"五段式讲（用上面综合实战的剧本，替换成自己的细节）。评分点在于：①有现场意识（dump 趁早）②工具链熟练（jstat/jmap/Arthas/MAT）③根因是代码而非参数④有预防动作（监控+规约+回归测试）。

---

**JVM 模块完成** ✅。接下来：[03-MySQL 深入](../03-MySQL深入/README.md)（大纲已备，含索引/事务/锁/日志四篇规划）。
