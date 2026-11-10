# mq-demo：教学配套可运行工程

Spring Boot 3.2 + RabbitMQ，对应教程四个阶段的全部代码演示。

## 运行前提

1. 启动 RabbitMQ（教程 stage1/02）：

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
```

2. 启动本工程：

```bash
cd mq/demo
mvn spring-boot:run
```

3. 管理界面：http://localhost:15672 （guest/guest），打开 **Queues** 页随时观察 Ready/Unacked。

## 演示清单（按学习顺序）

| 接口 | 验证什么 | 对应教程 | 预期现象 |
|------|---------|---------|---------|
| `GET /hello?msg=xxx` | Hello World 收发 | stage1/03 | 消费者日志立刻打印；Queues 页 Ready 从 1 变 0 |
| `GET /work?n=10` | 工作队列公平分发 | stage1/04 | 快消费者处理量远多于慢消费者（prefetch=1 能者多劳） |
| `GET /topic?key=order.pay.success` | Topic 通配符 | stage1/04 | 只有 `order.#` 队列收到（`*` 只匹配一个词） |
| `GET /topic?key=user.register` | Return 机制 | stage2/03 | 控制台 `[Return] 消息被退回!`（无队列匹配该 key） |
| `GET /idempotent` | 消费幂等 | stage3/02 | 业务执行 1 次 + 跳过 9 次 |
| `GET /dlq` | 死信链路 | stage3/05 | 失败重试 3 次 → 约 4 秒后 `[死信]` 兜底日志 |
| `GET /delay` | 延迟消息(TTL+DLX) | stage3/05 | 消息 5 秒后才被 `order.close.queue` 消费 |
| `POST /seckill?count=2000` | 秒杀削峰 | stage4/01 | 接入层瞬间完成 2000 次预扣；订单按消费速率匀速落库，无超卖 |

> 换 key 多打几次 `/topic`，对比两个队列的日志，是理解 Topic 模式最快的办法。

## 工程结构

```
src/main/java/com/example/mqdemo/
├── MqDemoApplication.java          启动类
├── config/
│   ├── RabbitConfig.java           拓扑声明 + Confirm/Return 回调 + Jackson 转换器
│   └── WorkContainerFactory.java   prefetch=1 公平分发容器工厂
├── controller/DemoController.java  全部演示入口
├── consumer/
│   ├── HelloReceiver.java          Hello World
│   ├── WorkReceivers.java          工作队列（快/慢消费者）
│   ├── TopicReceivers.java         order.* / order.# 两种绑定
│   ├── IdempotentConsumer.java     messageId 去重幂等（内存版，生产换 Redis+DB 唯一索引）
│   ├── DlqConsumers.java           必失败消费者 → 死信兜底消费者
│   └── OrderCloseConsumer.java     延迟消息消费（订单超时检查）
└── seckill/
    ├── SeckillService.java         模拟 Redis 预扣库存（CAS）+ 发 MQ
    ├── OrderConsumer.java          消费建单（模拟 DB 速率 + 幂等落库）
    └── SeckillMsg.java             消息体
```

## 环境对照（教学简化 vs 生产真实）

| demo 里 | 生产里 |
|---------|--------|
| `AtomicInteger`+CAS 模拟预扣库存 | Redis Lua 脚本原子 get+decr |
| `ConcurrentHashMap` 幂等去重/订单表 | Redis setnx + 数据库唯一索引/订单表 |
| TTL 队列 5 秒延迟 | 延迟插件任意延迟 / RocketMQ 延迟等级（30 分钟） |
| Confirm/Return 只打日志 | 记补偿表 + 定时任务重发（本地消息表） |

## 常见问题

- **连不上 broker**：确认容器在跑 `docker ps`，端口 5672 已映射。
- **队列声明冲突** `PRECONDITION_FAILED`：改过队列参数（durable/lazy 等）后旧队列还在，管理界面删掉旧队列重启应用。
- **发消息没反应**：检查控制台有无 `[Return]`——十有八九是 routing key 没匹配到队列。
