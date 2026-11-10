# 06 · Debezium CDC 与 Outbox 模式：本地消息表的工业级替代

mq-mall/community 里的"本地消息表"（业务+消息同事务写入，补偿任务重发）是手写版。**Debezium + Outbox 模式**是它的工业级形态：连"业务代码里写发送记录"这一步都省掉，从数据库的变更日志里自动捕获。

## 本地消息表的残余痛点（诚实复盘）

mq-mall 的方案（stage3/01）解决了"业务与消息原子性"，但代价：

1. **业务代码侵入**：每个要发消息的地方都要调 `sendInTransaction`（写发送记录）
2. **发送记录表膨胀**：需要清理 job、补偿 job
3. **读库写消息表**：发送记录写进了业务库（好在同库无跨库问题，但表多了）
4. 忘了调 publisher 的地方就是"静默不发"——坑三的教训

## CDC（Change Data Capture）：从"日志"里白嫖变更

MySQL 的 binlog 记录了每一行数据的变更——**Debezium 把 binlog 变成消息流**：

```
业务代码：只管 UPDATE/INSERT 业务表（完全不知道 MQ 的存在！）
              │
MySQL binlog（ROW 格式）
              │
Debezium（伪装成一个 MySQL 从库，读 binlog）
              │
   Kafka / RabbitMQ 消息流：每一行变更 = 一条消息（before/after 全量数据）
              │
下游消费者：更新缓存 → 发通知 → 同步 ES → ……
```

- **业务代码零侵入**：改数据就是改数据，发消息这件事彻底消失
- **不会丢变更**：binlog 是数据库的物理事实，应用崩溃/事务回滚都不会产生假消息（回滚的 binlog 不存在）
- 这正是 [stage2/04 对比表](../redis/stage2-底层原理/02-数据结构底层.md)里"账本模型"思想的变体：**从事实日志推导一切**

## Outbox 模式：给"发消息"这件事留一张表

业务里有时需要"发一条业务语义的消息"（如"订单已创建"事件），而不只是行变更。Outbox 标准化做法：

```sql
-- 业务事务内只写这张 outbox 表（与业务同事务，原子）
CREATE TABLE outbox_event (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(32),   -- 'Order'
    aggregate_id   VARCHAR(64),   -- '1001'
    type           VARCHAR(32),   -- 'OrderCreated'
    payload        JSON,          -- 事件内容
    created_at     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3)
);
```

```
业务事务：INSERT 订单 + INSERT outbox_event（原子，和本地消息表一样！）
Debezium 的 Outbox Event Router：专门监控这张表 → 变更转成结构化消息
    → 按 aggregate_id 路由（保证同一实体的消息有序！）
    → 消费后自动清理（Debezium 5.x+ 支持）
```

**与手写本地消息表的精确对比**：

| | 手写本地消息表（mq-mall） | Debezium Outbox |
|--|--------------------------|-----------------|
| 业务侵入 | 每处调 publisher | 只 INSERT 一张 outbox 表 |
| 发送机制 | 应用内补偿 job | binlog 捕获（进程死了变更也还在 binlog 里） |
| 消息顺序 | 按 messageId 幂等，不保证顺序 | **按 aggregate_id 分区，同实体天然有序** |
| 组件 | 零新增 | Kafka + Debezium Connector（重） |
| 适用 | 单体/轻量系统 | 微服务多团队、事件驱动架构 |

**选型结论**：单体或服务少 → 手写本地消息表（你已掌握，简单可控）；微服务化、事件驱动成为体系 → Debezium Outbox。

## 顺手联想到的两个 CDC 用途

1. **缓存一致性终解**（stage3/02 的 binlog 方案）：改 DB → Canal/Debezium → MQ → 删缓存，业务零侵入
2. **数据同步**：MySQL → Elasticsearch/数仓实时同步，替代"双写+对账"

## 落地路线（想真玩时的最小路径）

```
1. docker 起 Zookeeper + Kafka + Debezium（现成 docker-compose 官方示例）
2. MySQL 开 binlog（ROW 格式）——mq-mall 的 compose 加两个参数
3. 注册 connector：监控 mq_mall.t_mq_send_record 或自建 outbox 表
4. 消费 Kafka 消息：用 kafka-console-consumer 观察每一行变更
```

## 面试问法

**Q: 本地消息表和 Outbox/CDC 的关系？**
A: Outbox 模式是本地消息表的标准化命名（业务事务内写事件表保证原子）；本地消息表靠应用内补偿发送，Outbox+CDC（Debezium）靠数据库 binlog 捕获变更——业务代码零侵入、进程崩溃不丢（binlog 是物理事实）、按聚合根分区天然有序。代价是引入 Kafka+Debezium 基础设施。选型：单体手写、微服务化用 CDC。

**Q: Debezium 怎么保证不丢变更？**
A: 它伪装成 MySQL 从库，从 binlog（ROW 格式）读取；binlog 是数据库引擎在事务提交后写入的物理日志，回滚的事务不产生 binlog——所以"读到的变更必然已提交"，配合 Kafka 的位点管理实现 at-least-once，下游幂等消费。

---

**延伸阅读**：CQRS（命令查询职责分离）+ Event Sourcing（事件溯源）——把"事件"当作唯一事实来源、读模型由事件投影生成的架构流派，是本篇思想的终极形态。概念了解即可，落地需要事件存储与投影基建。
