# 07 · Testcontainers：在真实容器里跑集成测试

你的测试现状：`mvn verify` 只跑了编译（没有测试）。为什么大家不爱写测试？因为**集成测试环境太难搞**——要装 Redis、起 RabbitMQ、灌 MySQL……于是用 mock 糊弄，结果"全绿测试 + 上线就崩"。

**Testcontainers 改变了游戏规则**：测试代码里声明"我要一个 Redis"，框架自动用 Docker 起一个**真实的 Redis 容器**，测完自动销毁。测的是真件，不是仿真。

## 反差：Mock 测试 vs 真件测试

```java
// Mock 世界（自欺欺人）
when(redisTemplate.opsForValue().get("key")).thenReturn("cached");   // mock 了 Redis
// → Redis 挂了/key 写错了/序列化坏了——全测不出来！

// Testcontainers（真实世界）
@Container
static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));
// → 真实的 Redis 在跑：真连接、真序列化、真 TTL 过期、真 Lua 脚本
```

**你这一路踩的坑全都 mock 测不出来**：SETBIT 返回值语义反了（坑：签到）、双重序列化、SET 清 TTL——都是"真件行为与想象不符"的坑，mock 只会按你的想象返回。

## 落地：给 community 写一个真实测试（测幂等+签到位）

### 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

### 测试类（Boot 3.1+ 的 @ServiceConnection 自动接线，零配置）

```java
@SpringBootTest
@Testcontainers
class SignIntegrationTest {

    @Container
    @ServiceConnection                     // 自动把容器地址注入 spring.data.redis
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withInitScript("sql/schema.sql");   // 容器里自动建表

    @Autowired SignController signController;
    @Autowired StringRedisTemplate redis;

    @Test
    void 首次签到成功_重复签到幂等() {
        long uid = 999L;
        boolean first = redis.opsForValue().setBit("community:sign:" + uid + ":202611", 4, true);

        assertFalse(first);                        // 第一次：旧值 0 = 首签
        boolean second = redis.opsForValue()
                .setBit("community:sign:" + uid + ":202611", 4, true);
        assertTrue(second);                        // 第二次：旧值 1 = 已签

        // 这正是踩过的"SETBIT 返回值语义反了"坑的回归测试！
    }

    @Test
    void 幂等消费两层防线() {
        // 真实 Redis setnx + 真实 DB 唯一键，全链路验证 tryConsume/fail
    }
}
```

跑 `mvn test`：Docker 拉起 redis+mysql 容器 → Spring 起应用连**容器地址** → 跑测试 → 容器销毁。**每次测试都是全新干净环境**，测试之间互不污染。

## 组合进 CI（和上一章无缝衔接）

```yaml
- name: Integration tests
  run: mvn -B verify        # Testcontainers 在 GitHub Runner 上自动用 Docker 跑
```

GitHub 的 ubuntu-latest Runner **自带 Docker**——Testcontainers 在 CI 里开箱即用。这就是现代测试闭环：**push → 真容器真测试 → 绿了才算过**。

> [!TIP]
> **本机没开 Docker 怎么办**：给测试类加 `@Testcontainers(disabledWithoutDocker = true)`——无 Docker 环境测试自动 **Skipped（不是 Failed）**，CI 有 Docker 时照常真跑。community 的 `CommunityIntegrationTest` 就是这么写的（已验证：无 Docker 时 `Tests run: 3, Skipped: 3, BUILD SUCCESS`）。

## 写测试的分层策略（别 all-in 集成测试）

| 层 | 占比 | 跑什么 | 速度 |
|----|------|--------|------|
| 单元测试 | 70% | 纯逻辑（算法/状态机判断）| 毫秒 |
| 集成测试（Testcontainers） | 20% | 真 Redis/DB/MQ 的关键链路（幂等/事务/序列化）| 秒 |
| 端到端 | 10% | HTTP 全流程冒烟（起应用打接口）| 更慢 |

**集成测试只测"踩过坑的地方"**：幂等、状态机 CAS、序列化、Lua、过期语义——每次踩坑修完，写一个回归测试锁住它。你的踩坑实录就是最好的测试用例清单。

## Testcontainers 的姊妹场景

- **本地开发环境**：`@ServiceConnection` 也能用于开发期启动依赖（dev 服务化）
- **演示环境一键起**：mq-mall/community 的"三件套 compose + 应用"，就是手工版的同一思想
- **Spring Boot 3.1+ 开发期容器**（`@ServiceConnection` 用于 `spring-boot-devtools`）：开发时自动起依赖容器，替代手写 compose

## 面试问法

**Q: 你的项目里测试怎么做的？**
A: 三层——单元测试覆盖纯逻辑；集成测试用 Testcontainers 起真实 Redis/MySQL 容器（@ServiceConnection 自动接线），专门测踩过坑的关键链路：幂等两层防线、SETBIT 旧值语义、状态机 CAS、Lua 原子性；CI 里 GitHub Actions 自带 Docker 直接跑 Testcontainers，测试不过不允许合入。设计原则：mock 只用于单元测试的边角依赖，凡集成路径必真件——mock 测不出序列化/语义类缺陷。

---

**下一篇**：[08-响应式与虚拟线程路线之争](08-响应式与虚拟线程路线之争.md)。
