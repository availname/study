# 02 · Nacos 配置中心：配置的动态化

配置不用改代码重启就能生效——**动态线程池**（并发篇 05 的生产做法）的基础设施。

## 解决什么

| 传统方式（application.yml） | 痛点 |
|---------------------------|------|
| 打包进 jar | 改配置要重新构建部署 |
| 各环境手工维护 | 环境间漂移、配置泄漏风险 |
| 改完必须重启 | 线上限流阈值/开关无法即时调整 |

配置中心：配置存在 Nacos → 应用启动拉取 → **变更实时推送**（长轮询机制）→ @RefreshScope 的 Bean 属性热更新。

## 长轮询原理（面试核心）

客户端怎么"实时"知道配置变了？**长轮询（Long Polling）**：

```
客户端 → 发起请求监听配置变更 → 服务端 hold 住请求（最多 29.5 秒）
   ├── 期间配置变了 → 立即返回变更的 key 列表
   └── 29.5 秒没变 → 返回空 → 客户端立刻再发起（避免连接超时）
变更到达后客户端再拉一次全量配置 → 触发刷新
```

为什么是 29.5 秒：HTTP 连接默认超时 30 秒，留 0.5 秒缓冲。**对比**：推模式（实现复杂）vs 轮询（延迟大）vs **长轮询 = 准实时 + 简单可靠**——这也是很多"准实时"场景的标准解。

## 使用三步

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: localhost:8848
        file-extension: yaml
  application:
    name: community-service
# 配置文件 dataId = community-service.yaml（+ 共享的 common.yaml 可多文件）
```

```java
@RestController
@RefreshScope        // 配置变更时销毁重建 Bean，属性注入新值
public class ConfigDemoController {
    @Value("${community.welcome:默认值}")
    private String welcome;
    ...
}
```

配置变更 → Nacos 推送 → Spring 销毁 @RefreshScope Bean → 下次使用时以新配置重建。**@RefreshScope 只对注入时取值生效**——启动参数（如 mq-mall 的队列 TTL 声明）不能热更新（RabbitMQ 队列参数是注册时固化的），**能热更新的边界要清楚**。

## 动态线程池（组合实战，呼应并发篇 05）

```
Nacos 配置变更(maxPoolSize: 4→16)
  → 长轮询感知 → @RefreshScope 或 监听器
  → threadPool.setMaximumPoolSize(16)      // ThreadPoolExecutor 支持运行时改参
  → 告警/日志记录变更
```

美团方案（DynamicTp）就是把 ThreadPoolExecutor 参数全部托管到配置中心——**线程池参数是典型的"压测知道答案、线上需要微调"的配置**。

## 多环境与灰度

- **Namespace** 隔离环境（dev/test/prod 各一个）
- **Group** 隔离项目（DEFAULT_GROUP / COMMUNITY_GROUP）
- **灰度发布**：先推给指定 IP 的实例验证，再全量
- 回滚 = Nacos 控制台历史版本一键恢复（配置也有版本管理）

## 面试问法

**Q: Nacos 配置中心如何实现配置的动态刷新？**
A: 客户端对配置发起长轮询，服务端 hold 住最多 29.5 秒（略小于 HTTP 超时）；期间变更立即返回变更 key 列表，未变更返回空后客户端立刻重挂。变更后客户端拉取新配置，@RefreshScope 的 Bean 被销毁重建实现热更新。灰度发布按 IP 推送，历史版本可回滚。对比：推模式实现复杂、轮询延迟大，长轮询是准实时+简单的工程平衡点。

---

**下一篇**：[03-OpenFeign](03-OpenFeign.md)。
