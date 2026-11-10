# 01 · String：短信验证码登录与防刷

String 是默认选项，但把"验证码登录"这个最普通的场景**按真实产品要求做全**，String 的三个核心命令就全用上了。

## 需求（产品视角，一条都不能少）

| 功能点 | 数据操作形状 |
|--------|-------------|
| 验证码 5 分钟有效 | 写入 + TTL |
| 60 秒内不能重复发送（防骚扰） | 发送间隔判断 → NX 语义 |
| 每手机号每天最多发 10 条（防刷量） | 当日计数 + 上限 |
| 校验通过后验证码立即失效 | 删除 |
| 校验失败不删除（用户可能手滑输错一次） | 保留 |

**看到"操作形状"了吗**：①带TTL写 ②不存在才写 ③计数+上限 ④删除——全是 String 的活。

## 方案与命令

三个 key，各司其职：

```bash
# ① 验证码本体：5 分钟 TTL
SET captcha:login:13800001111 8848 EX 300

# ② 发送间隔锁：60 秒内二次发送被拒（NX = 已存在就失败，天然实现"间隔"）
SET captcha:interval:13800001111 1 EX 60 NX

# ③ 当日发送计数：INCR + 首次设置 TTL（跨天自动重置）
INCR captcha:count:13800001111:20261105
# 只有第一次 INCR（返回1）时才设置过期——key 生命周期精确到"当天剩余秒数"
```

完整发送逻辑（Java）：

```java
public String send(String phone) {
    String ip = ...;
    // 防刷1：60 秒间隔（NX 抢锁语义复用）
    Boolean first = redis.opsForValue().setIfAbsent(
            "captcha:interval:" + phone, "1", Duration.ofSeconds(60));
    if (Boolean.FALSE.equals(first)) {
        throw new BizException("发送太频繁，请60秒后再试");
    }
    // 防刷2：当日上限
    String countKey = "captcha:count:" + phone + ":" + today();
    Long count = redis.opsForValue().increment(countKey);
    if (count == 1) {
        // 只有第一次计数才设过期：过期点 = 当天 24:00（简化版：24 小时）
        redis.expire(countKey, Duration.ofHours(24));
    }
    if (count > 10) {
        throw new BizException("今日发送次数已达上限");
    }
    // 验证码本体
    String code = String.valueOf(100000 + random.nextInt(900000));
    redis.opsForValue().set("captcha:login:" + phone, code, Duration.ofMinutes(5));
    smsGateway.send(phone, code);
    return code;
}
```

**坑就在 `increment` + `expire` 这对组合**：两步不是原子的——`INCR` 后进程崩溃，count key 永不过期（残留脏计数）。修法与秒杀同款，Lua 原子化：

```lua
-- incr_with_ttl.lua：INCR + 首次设 TTL 原子执行
local c = redis.call('INCR', KEYS[1])
if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
return c
```

这个"INCR+首次EXPIRE"的 Lua 是**通用限流组件**，滑动窗口之外的固定窗口限流全靠它（登录防刷、接口防刷、下载限次）。

## 校验逻辑（一次性消费）

```java
public boolean verify(String phone, String input) {
    String key = "captcha:login:" + phone;
    String saved = redis.opsForValue().get(key);
    if (saved == null) return false;              // 过期/未发送
    if (!saved.equals(input)) return false;       // 输错不删（允许重试）
    redis.delete(key);                            // 只有成功才删——一次性
    return true;
}
```

并发场景"GET 后 DEL"有窗口（两个请求同时 GET 都通过）→ 校验和删除用 Lua 原子化（redis-demo 的 `CaptchaController` 就是这个实现）。

## 生产增强（加分项）

| 增强 | 做法 |
|------|------|
| IP 维度防刷 | 同样的 count key 换成 `captcha:count:ip:1.2.3.4:日期`，防一人多号 |
| 图形验证码前置 | 发短信前先过图形验证码，把刷量成本提高到"人工"级别 |
| 发送记录审计 | 发送流水落 DB/MQ，Redis 只做实时闸门 |
| Redis 挂了 | 降级：验证码直发不限次（牺牲防刷保可用），登录校验走 DB 备份表 |

## 关键认知（这个场景教给你的事）

**Redis 在这里是"闸门"不是"账本"**：验证码丢一次用户重发即可，所以敢放 Redis；但**发送审计流水必须落 DB/MQ**（客服对账、攻击溯源）。数据放哪，取决于"丢了会怎样"。

## 面试问法

**Q: 如何防止短信验证码接口被刷？**
A: 三层闸门——①SET NX EX 60 实现"60秒发送间隔"（原子，防骚扰）；②INCR+首次EXPIRE 的当日计数限 10 条（Lua 保证原子，防刷量）；③IP 维度同样计数 + 图形验证码前置提高刷量成本。验证码本体 5 分钟 TTL、校验成功原子删除。审计流水落库，Redis 只做实时闸门。

---

**下一篇**：[02-Hash-电商购物车](02-Hash-电商购物车.md)。
