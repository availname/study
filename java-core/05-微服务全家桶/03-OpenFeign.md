# 03 · OpenFeign：声明式的服务调用

调用远程服务像调本地方法——写个接口，Feign 用动态代理帮你变成 HTTP 请求。

## 原理一句话

```java
@FeignClient(name = "user-service")            // name = Nacos 服务名（自动服务发现+负载均衡）
public interface UserClient {
    @GetMapping("/api/users/{id}")
    UserDTO getUser(@PathVariable("id") Long id);
}

// 注入直接用——背后是 Feign 动态代理：
UserDTO user = userClient.getUser(1L);          // HTTP GET http://user-service/api/users/1
```

调用链：接口方法 → 动态代理 → 按 Nacos 服务名取实例列表 → **负载均衡器选一个**（Spring Cloud LoadBalancer，轮询/随机）→ 构造 HTTP 请求 → 编码发送 → 解码响应。**手写的 RestTemplate + 服务发现 + 序列化代码全部消失**。

## 超时与重试：必须显式配置（默认值是坑）

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:                    # 全局；也可按服务名 user-service 单独配
            connect-timeout: 2000     # 建连超时 2s
            read-timeout: 5000        # 读超时 5s
            retryer: never            # 生产默认不自动重试！
```

> [!CAUTION]
> **Feign 默认的重试行为会放大写请求**：POST 创建订单被重试 = 可能创建两单。写接口的正确姿势——**调用方不盲目重试，下游接口做幂等**（stage3/02 的 message_id 唯一索引），两层一起才安全。这与 Redis 教学"Redis 锁挡 99%、DB 兜底"的分层思想一致。

## 与拦截器：token 怎么传给下游

Feign 调用默认**不带调用方的请求头**（新请求）——用户身份传递要用 RequestInterceptor：

```java
@Component
public class FeignAuthInterceptor implements RequestInterceptor {
    public void apply(RequestTemplate template) {
        // 从当前请求的 ThreadLocal 上下文取出 token/traceId，塞进下游请求头
        template.header("Authorization", currentToken());
        template.header("X-Trace-Id", traceId());   // 追踪传播（stage6/04 也可自动传播）
    }
}
```

**这正是微服务上下文传递的标准痛点**：登录态/traceId 要手动（或框架自动）跨服务传递——ThreadLocal 跨不了线程（更跨不了进程），所以要有显式的传递通道。

## 熔断集成（联动 05 Sentinel）

给 FeignClient 加 `fallback`，下游挂了不再抛异常而是走降级逻辑：

```java
@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient { ... }

@Component
public class UserClientFallback implements UserClient {
    public UserDTO getUser(Long id) {
        return UserDTO.unknown(id);        // 降级返回"游客"视图，页面照常渲染
    }
}
```

降级返回**兜底数据而不是报错**——页面局部降级好过整页崩掉（熔断原理在 05 Sentinel 篇）。

## 与 MQ 的分工（呼应教学主线）

| 调用类型 | 用什么 |
|---------|--------|
| 需要**同步拿结果**（下单要返回订单号） | OpenFeign 同步调用 |
| **不需要结果**（发通知、记审计、刷新缓存） | MQ 异步（可靠 + 削峰 + 解耦） |

**判据：调用方是否需要即时结果、下游慢是否影响上游体验。** community 里"审核通过后通知作者"就是 MQ 异步的例子（不需要等通知发完）。

## 面试问法

**Q: OpenFeign 的工作原理？要注意什么？**
A: 接口+注解 → Feign 动态代理生成 HTTP 客户端：按服务名从 Nacos 拉实例列表 → LoadBalancer 负载均衡选实例 → 构造请求（编码）→ 发送 → 解码响应。要点：①超时显式配置且**写接口禁自动重试**（重试放大写请求，下游幂等兜底）；②RequestInterceptor 传递登录态/traceId；③集成 Sentinel 做 fallback 降级返回兜底数据。同步 Feign 与 MQ 异步的分工：需要即时结果走 Feign，不需要结果走 MQ。

---

**下一篇**：[04-Gateway 网关](04-Gateway网关.md)。
