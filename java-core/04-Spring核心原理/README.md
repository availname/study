# 04 · Spring 核心原理（★★）✅ 5/5 完成

规划 5 篇——项目里"用过但没懂"的部分系统化：

| 篇 | 主题 | 状态 | 核心考点 |
|----|------|------|---------|
| [01](01-IoC容器与Bean生命周期.md) | IoC 与 Bean 生命周期 | ✅ | 十一步生命周期 / **三级缓存循环依赖** / AOP 代理生成位置 |
| [02](02-AOP原理.md) | AOP 原理 | ✅ | JDK vs CGLib / 五种通知顺序 / **代理失效全家福** |
| [03](03-事务原理与失效八连.md) | 事务原理 ★ | ✅ | AOP+ThreadLocal 连接绑定 / 传播行为七种 / **失效八连**（mq-mall 踩过的坑全在内） |
| [04](04-SpringMVC请求流程.md) | Spring MVC | ✅ | 五步流程 / 参数绑定六来源 / 拦截器 vs 过滤器 / 中文 400 真相 |
| [05](05-常用扩展机制.md) | 扩展机制 | ✅ | BeanPostProcessor / Event+AFTER_COMMIT / 条件装配（Boot 自动装配原理） |

## 与已有教学/项目的连接点

- **事务失效八连**里的"自调用"：mq-mall `SeckillOrderTxWorker` 拆独立 bean 的原因（并发篇自调用代理失效同款）
- **事务+MQ 原子性**：本地消息表的 `sendInTransaction` 依赖 `TransactionSynchronizationManager`——事务原理篇讲清 afterCommit 时机
- **Bean 生命周期**：`@PostConstruct`（mq-mall 秒杀初始化 Redis 库存）在哪个阶段执行
