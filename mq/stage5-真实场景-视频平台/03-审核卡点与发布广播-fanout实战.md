# 03 · 审核卡点与发布广播：fanout 实战

对应代码：`MediaPipelineConsumers.onTranscoded / finishHumanAudit / onAudited / onRecommend / onNotify`

## 审核状态机：AI 秒审 + 人工卡点

真实的审核不是"过/不过"二值——AI 有第三种输出：**拿不准**。demo 用标题关键词模拟三种结果：

```java
if (title.contains("BLOCK"))  → CAS(AUDITING→REJECTED)   // 确定违规，直接拒绝
if (title.contains("HUMAN"))  → CAS(AUDITING→HUMAN_AUDIT) // AI 不确定，卡在人工队列
else                          → CAS(AUDITING→AUDITED) → 发 media.audited 继续
```

**人工审核的处理方式是本设计的点睛之处**：任务停在 `HUMAN_AUDIT` 状态，**消息已被正常消费掉**——审核员什么时候处理都行（可能是几小时后），不会有消息挂着等。人工做出决定后，由**人工端点**推送后续消息：

```java
// POST /api/videos/{id}/audit?pass=true
pipeline.finishHumanAudit(event, pass);   // CAS 推进 + 发 media.audited，管道继续流动
```

这就是**"MQ 消息驱动流转 + DB 状态持久卡点"**的组合：消息是"推动力"，状态是"事实"，两者解耦后，人工介入、超时升级、批量处理都变成了"改状态 + 补发消息"而已。

> 对比反模式：如果人工审核也挂在消费者里"等审核员点击"，连接会挂死、prefetch 占满、重启丢上下文。**长等待永远不要用消息阻塞表达，要用状态 + 事件表达。**

## 发布广播：fanout 的教科书用法

审核通过后，"视频上线了"这件事要让多个下游知道：

```
media.audited.queue → ③上架消费者
                          └→ media.published.fanout ──┬── media.recommend.queue（推荐索引）
                                                       └── media.notify.queue（粉丝推送）
```

**为什么用 fanout 而不是让上架消费者直接调三个服务？**

| 直接调用 | fanout 广播 |
|---------|------------|
| 上架代码知道所有下游（耦合） | 上架只管"发布"事件（解耦，stage1/01 原则） |
| 加"数据分析"下游要改上架代码 | 新服务绑一个新队列，**零改动接入** |
| 推荐服务挂了阻塞上架 | 各队列独立消费，一个挂不影响其它 |
| 推荐要重放历史？做不到 | 队列里的消息天然可重放（重新入队） |

这和 stage1/01 "用户注册事件" 是同一个模式，但注意差异：**注册广播丢了无所谓，"发布广播"的推荐索引入库丢了会导致视频搜不到**——所以 demo 里 media.* 队列全部配了死信交换机（`dlx.exchange`），下游处理失败有兜底。广播的"尽力而为"和"必须送达"要看下游重要性，按下游配可靠性。

## 百万粉丝推送的削峰（真实难题）

UP 主有 500 万粉丝，一条"新视频"事件来了，推送怎么做？

**反面教材：一次全推**

```
[粉丝通知] 消费者收到事件 → 查 500w 粉丝 → 循环调用推送网关
→ 推送网关被打挂（它是公共组件！其他业务的推送也一起死）
→ 消费超时 → 消息重投 → 又全推一遍 → 用户收到 500 万条重复推送
```

**正确姿势：分批 + 限速 + 幂等**

```java
@RabbitListener(queues = "media.notify.queue")
public void onNotify(VideoEvent event) {
    long cursor = 0;
    do {
        List<Long> fans = fanService.loadFans(event.videoId(), cursor, 5000); // 分页拉粉丝
        pushGateway.batchSend(fans, event);   // 批量接口，一次 5000
        cursor = 下一次偏移;
        Thread.sleep(100);                    // 限速：保护推送网关（stage3/04 消费保护下游）
    } while (还有粉丝);
}
```

配套要点：

- **批量消费**：`BatchListener` 一次拿 N 条事件合并处理（stage3/04 的批量套路）；
- **推送幂等**：推送记录表 `(video_id, user_id)` 唯一键，网关超时重试不重复打扰用户；
- **可中断**：分页游标存 Redis，消费者重启接着推；
- **敏感营销合规**：全量推送前先推 1% 灰度观察投诉率。

**这和秒杀削峰（stage4/01）是同一个思想的两个方向**：秒杀是"洪峰请求排队进 DB"，推送是"一个事件放大成洪峰出口"——**MQ 在入口和出口都能当水库**。

## demo 里观察 fanout

提交一个正常视频，等 PUBLISHED 后看日志（同 videoId 出现 5 条跨 3 个环节的日志）：

```
[转码] 完成 {videoId}
[审核] AI 审核通过 {videoId}
[上架] 已发布并广播 {videoId}
[推荐索引] 新视频入库可检索: {videoId}     ← fanout 下游 1
[粉丝通知] UP主《...》发布已推送给粉丝        ← fanout 下游 2
```

**动手扩展练习**：给 fanout 加第三个下游——`media.points.queue` 消费"发布完成"给 UP 主加创作积分（复用 `t_points_log` 唯一索引幂等）。体会"零改动接入"是不是真的零改动。

---

**下一篇**：[04-失败语义与场景方法论](04-失败语义与场景方法论.md)。
