# 04 · Gateway 网关：微服务的统一大门

所有外部请求先经过网关，再到各服务。网关做四件事：**路由、鉴权、限流、观测**——把"每个服务都要做的事"收口到一个地方。

## 架构位置

```
外部请求 → [Gateway 8080] ──路由──→ community-service (8082)
                │                    user-service    (8083)
                │──鉴权(验token)      order-service   (8084)
                │──限流(Sentinel/Redis)
                └──日志/traceId 生成（stage6/04 的 traceId 从这里诞生）
```

## 路由三要素：谓词 + 过滤器

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: community-route
          uri: lb://community-service        # lb = 从 Nacos 负载均衡
          predicates:                         # 什么请求走这条路由
            - Path=/api/posts/**,/api/sign/**
            - Method=GET,POST
          filters:                            # 路由前/后做什么
            - StripPrefix=0
            - AddResponseHeader=X-Source, gateway
```

- **谓词 Predicate**：判断是否匹配（Path/Method/Header/时间窗……）
- **过滤器 Filter**：匹配后加工请求/响应（前置改头、后置统计；全局过滤器做鉴权）

## 鉴权前置：community 的 token 校验上移

community 的每个接口现在都传 `?token=`——微服务化后统一收到网关的全局过滤器：

```java
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/api/users/login")) return chain.filter(exchange);   // 白名单放行

        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (!tokenService.valid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();      // 直接 401，不到服务
        }
        exchange.getRequest().mutate().header("X-User-Id", parseUser(token)).build();
        return chain.filter(exchange);                         // 用户身份转发给下游
    }
    public int getOrder() { return -100; }    // 数字越小越先执行
}
```

**好处**：登录校验写一次全服务生效；服务内部专注业务；非法请求在门口就被挡（省下游资源）。

## 限流：网关层挡住第一波洪峰

呼应 [mq stage4/01 秒杀漏斗②层](../../mq/stage4-高并发实战/01-削峰填谷-秒杀系统.md)：

```yaml
filters:
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 500    # 每秒补充 500 个令牌
      redis-rate-limiter.burstCapacity: 1000   # 突发容量 1000
      key-resolver: "#{@ipKeyResolver}"        # 按 IP 限（也可按用户/接口）
```

令牌桶算法（匀速补令牌 + 桶容量容忍突发）——**Redis 教学 stage3/01 的固定窗口限流更精细的版本**。网关限流保护的是**全后端**，Sentinel（下一篇）保护的是**单个服务/接口**。

## 网关的三条纪律

1. **网关只做横切**：路由/鉴权/限流/日志——**业务逻辑一律下沉服务**（网关里写业务=新的上帝类）
2. **网关是无状态轻组件**：多实例水平扩容（它的可用性 = 全系统入口的可用性）
3. **traceId 在网关生成**：进入系统的第一个点生成 trace 头一路传递（stage6/04 链路完整性的起点）

## 与 community 的对应

| community 现状 | 微服务化后 |
|---------------|-----------|
| 每个接口 `?token=` 校验 | 网关统一 Authorization 校验 + X-User-Id 转发 |
| 应用内 @ExceptionHandler | 网关级错误页 + 服务内业务异常并存 |
| 单应用无路由 | /api/posts/** → community-service 等路由表 |

## 面试问法

**Q: 网关的作用？如何做统一鉴权和限流？**
A: 四职责：路由（谓词匹配+lb 负载均衡到 Nacos 服务）、鉴权（全局过滤器验 token，白名单放行，身份转发 X-User-Id）、限流（RequestRateLimiter 令牌桶按 IP/用户/接口维度，Redis 存令牌）、观测（生成 traceId、日志）。纪律：网关只做横切不做业务；多实例无状态扩容；秒杀场景网关限流是四层漏斗的第二层。

---

**下一篇**：[05-Sentinel 熔断限流](05-Sentinel熔断限流.md)。
