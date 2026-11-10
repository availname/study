# 02 · RabbitMQ 安装与管理界面

你有 Docker，直接用 Docker 装（推荐）。没有 Docker 的 Windows 原生安装放最后。

## 方式一：Docker（推荐）

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
```

参数解释：

| 参数 | 含义 |
|------|------|
| `-d` | 后台运行 |
| `--name rabbitmq` | 容器名，方便以后 `docker logs rabbitmq`、`docker restart rabbitmq` |
| `-p 5672:5672` | **AMQP 端口**——Java 客户端连这个 |
| `-p 15672:15672` | **管理界面端口**——浏览器访问这个 |
| `rabbitmq:3.13-management` | 带 management 插件的镜像，才 有管理界面 |

验证：

```bash
docker ps                       # 看容器是否 Up
docker logs rabbitmq --tail 20  # 看到 "Server startup complete" 即成功
```

打开浏览器：`http://localhost:15672`，默认账号密码 **guest / guest**。

> 注意：guest 账号**只允许本机登录**。容器里运行时你从宿主机浏览器访问 15672 是通过端口映射，浏览器请求源头是 localhost，所以 guest 能登录。但如果你的 RabbitMQ 部署在远程服务器，guest 登录会被拒绝，需要创建新用户（见下文命令行部分）。

## 端口一览

| 端口 | 用途 |
|------|------|
| 5672 | AMQP 0-9-1 协议端口，**程序连接用** |
| 15672 | HTTP 管理界面 |
| 25672 | Erlang 分布式节点间通信（集群才用） |
| 5671 | AMQP over TLS（开启 SSL 时） |
| 1883 | MQTT 插件（了解即可） |

## 管理界面导览（重点）

登录后顶部有 6 个 tab，按使用频率讲：

### 1. Overview
全局健康：节点状态、内存/磁盘占用、消息进出速率（`Message rates` 折线图）。排查问题的第一站。

### 2. Connections
每个 TCP 连接一行。看看有哪些应用连着、心跳是否正常。

### 3. Channels
一个 Connection 里可以开多个 Channel（后面原理篇细讲）。每个 Channel 有自己的 `Unacked`（未确认消息）数——**消费者卡住时先看这里**。

### 4. Exchanges
所有交换机。默认有几个内置的：
- `(AMQP default)`：默认交换机，routing key = 队列名，直接路由
- `amq.direct` / `amq.fanout` / `amq.topic`：内置的direct/fanout/topic交换机

你可以在页面上**手动建交换机**练手：Name 填 `test.topic`，Type 选 `topic`，点 Add exchange。

### 5. Queues（最常用）
所有队列，每行显示两个关键数字：

| 列 | 含义 |
|----|------|
| **Ready** | 已到达、等待被消费的消息数 |
| **Unacked** | 已投给消费者、但还没收到 ACK 的消息数 |

```
消费正常：Ready ≈ 0，Unacked ≈ prefetch 数（比如 1~50）
消费者挂了：Ready 持续上涨，Unacked = 0
消费者卡住（处理慢/死锁）：Ready 不动，Unacked = prefetch 上限
消费太快把下游打挂：都不动但下游报警（所以消费也要有节制）
```

点进一个队列，往下翻有 **Bindings**、**Publish messages**（手动发消息）、**Get messages**（手动取消息）——学习阶段的好玩具：
- 手动发一条消息 → 观察消费者日志立刻收到
- 暂停消费者 → 手动发消息 → 看 Ready 变 1 → 重启消费者 → 看 Ready 变 0

### 6. Admin
用户、虚拟主机（Virtual Hosts）、权限管理。建项目时习惯按项目建 vhost 隔离（如 `/order`、`/seckill`）。

## 命令行（进容器执行）

```bash
docker exec -it rabbitmq rabbitmqctl list_queues          # 列出队列
docker exec -it rabbitmq rabbitmqctl list_exchanges       # 列出交换机
docker exec -it rabbitmq rabbitmqctl list_users           # 列出用户
```

创建用户（远程访问必需）：

```bash
docker exec -it rabbitmq rabbitmqctl add_user app secret123
docker exec -it rabbitmq rabbitmqctl set_user_tags app administrator
docker exec -it rabbitmq rabbitmqctl set_permissions -p / app ".*" ".*" ".*"
# 三个 ".*" 分别是：配置权限、写权限、读权限（正则）
```

## 方式二：Windows 原生安装（没 Docker 才看这个）

1. RabbitMQ 依赖 Erlang 运行时，且版本必须匹配：查官方兼容表 https://www.rabbitmq.com/which-erlang.html
2. 装 Erlang：https://www.erlang.org/downloads （选对应版本，一路下一步）
3. 装 RabbitMQ：https://github.com/rabbitmq/rabbitmq-server/releases 下载 `rabbitmq-server-windows-x.x.x.zip` 解压
4. 启用管理插件：
   ```bash
   rabbitmq-plugins enable rabbitmq_management
   ```
5. 启动：开始菜单里的 `RabbitMQ Service - start`，或 `rabbitmq-server.bat`

## 常见问题

| 现象 | 原因/解法 |
|------|----------|
| 15672 打不开 | 镜像没用 `-management` 后缀的 tag；或管理插件没启用 |
| guest 远程登录 401 | guest 仅限本机，建新用户 |
| 程序连 5672 报 `connection refused` | 容器没起来，`docker logs rabbitmq` 看日志 |
| Java 报 `AUTHENTICATION_FAILED` | 用户名密码错，或该用户没有对应 vhost 权限 |

---

**动手任务**：启动容器 → 登录管理界面 → 在 Queues 页手动建一个队列 `test.queue` → 手动 Publish 一条消息 → 手动 Get 出来看。你已经在用手玩 MQ 了。

**下一节**：[03-HelloWorld-第一个生产者消费者](03-HelloWorld-第一个生产者消费者.md)——换成代码来玩。
