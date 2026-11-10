# 05 · Spring Boot 整合 Redis

实际工作的标准姿势：Spring Data Redis + `RedisTemplate`。完整可运行代码在 [redis-demo](../redis-demo/README.md)。

## 依赖与配置

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6381
      # password: xxx
      lettuce:
        pool:
          max-active: 16      # 连接池（需要 commons-pool2 依赖）
          max-idle: 8
```

## 第一课：序列化器必须配（不然存进去是乱码）

默认的 `RedisTemplate<Object,Object>` 用 **JDK 序列化**——存进 Redis 是 `\xac\xed\x00\x05...` 乱码，redis-cli 没法看，跨语言不兼容。**标配：key 用 String、value 用 Jackson**：

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        Jackson2JsonRedisSerializer<Object> json =
                new Jackson2JsonRedisSerializer<>(Object.class);
        template.setKeySerializer(RedisSerializer.string());       // key: 字符串
        template.setHashKeySerializer(RedisSerializer.string());
        template.setValueSerializer(json);                          // value: JSON
        template.setHashValueSerializer(json);
        template.afterPropertiesSet();
        return template;
    }
}
```

> 只存字符串可用 `StringRedisTemplate`（Boot 自动装配，key/value 都是字符串），最简单也最常见。

## 五大类型对应五组 Ops

```java
redisTemplate.opsForValue()   // String
redisTemplate.opsForHash()    // Hash
redisTemplate.opsForList()    // List
redisTemplate.opsForSet()     // Set
redisTemplate.opsForZSet()    // ZSet
```

### String：验证码 + 计数器

```java
@Service
public class CaptchaService {
    private final StringRedisTemplate redis;   // 构造器注入

    public void send(String phone) {
        String code = String.valueOf(new Random().nextInt(900000) + 100000);
        // SET + EX 一步到位，5 分钟过期
        redis.opsForValue().set("captcha:login:" + phone, code, Duration.ofMinutes(5));
    }

    public boolean verify(String phone, String input) {
        String key = "captcha:login:" + phone;
        String saved = redis.opsForValue().get(key);
        if (saved == null) return false;          // 过期或不存在
        redis.delete(key);                        // 一次性使用
        return saved.equals(input);
    }
}
```

### Hash：购物车

```java
// 加购/修改数量（原子）
redis.opsForHash().increment("cart:" + userId, skuId, 1);
// 读单个
Object count = redis.opsForHash().get("cart:" + userId, skuId);
// 整车
Map<Object, Object> cart = redis.opsForHash().entries("cart:" + userId);
// 删
redis.opsForHash().delete("cart:" + userId, skuId);
```

### ZSet：排行榜

```java
redis.opsForZSet().incrementScore("rank:game", playerName, 100);   // 加分
Set<String> top3 = redis.opsForZSet().reverseRange("rank:game", 0, 2);      // 前三
Long rank = redis.opsForZSet().reverseRank("rank:game", playerName);        // 我排第几
Double score = redis.opsForZSet().score("rank:game", playerName);
```

### Set / List 同理

```java
redis.opsForSet().add("lottery", "u1", "u2");
redis.opsForSet().intersect("friends:u1", "friends:u2");     // 共同好友
redis.opsForList().leftPush("feed:1001", "新动态");
redis.opsForList().range("feed:1001", 0, 9);
```

## 事务与原子性：RedisTemplate 没有"事务感"

Redis 的 `MULTI/EXEC` 在 Spring 里是 `SessionCallback`，但**实际业务里更常用的是 Lua 脚本**（比 MULTI 灵活，能"判断后决定写什么"，且全程原子）：

```java
// Lua 原子实现"验证码校验+删除"（对比上面的 get-then-delete 有竞态窗口）
private static final DefaultRedisScript<Long> VERIFY_SCRIPT = new DefaultRedisScript<>("""
    if redis.call('get', KEYS[1]) == ARGV[1] then
        redis.call('del', KEYS[1])
        return 1
    end
    return 0
    """, Long.class);

Long r = redis.execute(VERIFY_SCRIPT, List.of(key), inputCode);
```

**结论**：涉及"检查+操作"的复合逻辑，一律 Lua（stage4/01 秒杀就是这个套路的巅峰）。

## demo 工程

[redis-demo](../redis-demo/README.md) 把本节全部内容做成 HTTP 接口：

| 接口组 | 演示 |
|--------|------|
| `/types/**` | 五大类型读写 |
| `/captcha/**` | 验证码发/验（String+TTL+Lua） |
| `/rank/**` | 排行榜加分/前三/我的名次 |
| `/lock/**` | 分布式锁抢锁/释放（stage3/03 配套） |
| `/seckill/**` | Lua 秒杀不超卖（stage4/01 配套） |
| `/stream/**` | Stream 队列收发（stage4/03 配套） |

启动方式见其 README（Docker 起 Redis 6381 → `mvn spring-boot:run`）。

## 面试问法

**Q: RedisTemplate 序列化怎么配？为什么？**
A: 默认 JDK 序列化存 Redis 是二进制乱码、不可读、跨语言不兼容。标配 StringRedisSerializer 存 key（保持可读、支持 SCAN 前缀匹配），Jackson JSON 存 value（可读、跨语言、体积小）。纯字符串场景直接用 StringRedisTemplate 最轻。

---

**Stage 1 完成**。接下来 [Stage 2-底层原理](../stage2-底层原理/01-线程模型与IO多路复用.md)——拆开看 Redis 为什么快、怎么不丢数据。
