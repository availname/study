# 05 · 微服务全家桶（★★ SpringCloud Alibaba）✅ 7/7 完成

> 🎬 **配套交互演示**：[diagrams/microservice-arch.html](../../diagrams/microservice-arch.html)——10 组件全景图：点组件看说明，"请求的旅程"动画演示 网关→Nacos→Feign→Redis/DB→MQ 全链路。

对接工作区的 **pmhub 项目**（SpringCloud Alibaba 体系）。每一篇都与已有教学强连接：

| 篇 | 组件 | 解决什么 | 与已有教学的连接 |
|----|------|---------|-----------------|
| [01](01-Nacos注册中心.md) | Nacos 注册中心 | 服务发现：谁在哪 | AP/CP 取舍 vs Redis 主从异步复制 |
| [02](02-Nacos配置中心.md) | Nacos 配置中心 | 配置动态化（动态线程池） | 长轮询 vs Redis 长轮询思想；并发篇 05 |
| [03](03-OpenFeign.md) | OpenFeign | 声明式服务调用 | **重试放大写请求→下游幂等**（stage3/02） |
| [04](04-Gateway网关.md) | Gateway | 路由/鉴权/限流前置 | community token 校验上移；令牌桶（stage3/01） |
| [05](05-Sentinel熔断限流.md) | Sentinel | 限流/熔断/降级 | 秒杀②层（mq stage4/01）；多级缓存降级（stage3/01） |
| [06](06-Seata分布式事务.md) | Seata | 准实时强一致 | **对比本地消息表/事务消息**（mq stage4/03）；CAS 防脏写（并发04） |
| [07](07-XXL-Job分布式任务调度.md) | XXL-Job | 分布式任务调度：多实例不重复跑+分片广播 | mq-mall 的 @Scheduled 扫单Job（多实例事故引子）；延迟队列分工（mq stage4/02） |

## 前置声明

微服务是**有代价的架构选择**：网络调用替代本地调用（延迟/失败率上升）、分布式事务、链路追踪复杂度。mq-mall/community 的单体验证了业务可行性——拆微服务的时机是"团队规模与部署独立性诉求"驱动，不是技术炫技。
