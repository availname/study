# 01 · Nacos 注册中心

微服务第一问："服务 A 要调服务 B，怎么知道 B 在哪？"——注册中心解决它。

## 核心模型

```
服务B 启动 → 向 Nacos 注册（服务名+IP+端口+元数据）+ 每 5 秒心跳
服务A 要调 B → 按【服务名】向 Nacos 查询 → 拿到 B 的实例列表 → 负载均衡选一个调用

Nacos
 ├── 服务B-实例1 (192.168.1.10:8080) 健康✓
 ├── 服务B-实例2 (192.168.1.11:8080) 健康✓
 └── 服务B-实例3 (192.168.1.12:8080) 心跳超时 → 标记不健康 → 摘除
```

**服务的"地址"从配置文件里消失了**——扩容/缩容/换机器，调用方零感知。这就是"服务发现"。

## 两种实例与 AP/CP（面试核心）

| | 临时实例（默认） | 持久实例 |
|--|----------------|---------|
| 心跳 | 客户端主动每 5s 上报 | 服务端主动探测 |
| 不健康处理 | 超时摘除 | 不摘除，只标记 |
| 存储 | 内存（AP，Distro 协议） | 磁盘（CP，Raft） |
| 适用 | **常规微服务实例** | 配置类/DB 类不会频繁变动的服务 |

**CAP 取舍**：服务发现要的是"可用"（拿不到最新列表也比拿不到服务强）→ Nacos 对临时实例走 **AP（Distro 协议，最终一致）**；配置/持久实例要"一致"→ **CP（Raft）**。一个组件两种模式按数据类型切换——这是它对 Eureka（纯 AP）和 ZooKeeper（纯 CP）的超越点。

## Spring Cloud Alibaba 接入（三步）

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```
```yaml
spring:
  application:
    name: community-service        # 服务名=发现的关键字
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
```

启动后自动注册；OpenFeign 按服务名调用（03 篇）。`docker run -d -p 8848:8848 nacos/nacos-server:v2.3.0` 单机起步。

## 保护阈值与推送

- **不健康实例摘除后，若健康实例占比低于保护阈值（默认 0）**，Nacos 会**把不健康实例也返回**——"宁可返回旧地址不可没有服务"（AP 思想的落地），生产可设 0.5
- 列表变更时 **UDP 推送 + 客户端定时拉取**双通道，变更感知秒级

## 与你项目的连接

- mq-mall/community 是单体，不需要注册中心——**拆微服务的第一步就是接入它**
- 你工作区的 pmhub（SpringCloud Alibaba）里 Nacos 是全家桶的地基：配置中心+注册中心一体的服务端

## 面试问法

**Q: Nacos 作为注册中心的核心原理？临时实例和持久实例的区别？**
A: 临时实例：客户端每 5s 心跳上报，超时摘除，内存存储走 AP/Distro 协议（最终一致、可用优先），适配常规微服务；持久实例：服务端探测，CP/Raft，适配配置类数据。保护阈值防止健康实例不足时全部摘除。选型对比：Eureka 纯 AP（已停更）、ZooKeeper 纯 CP（master 挂时不可用）——Nacos 按"临时/持久"同时支持 AP/CP。

---

**下一篇**：[02-Nacos 配置中心](02-Nacos配置中心.md)。
