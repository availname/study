# 02 · Hash：电商购物车

购物车是 Hash 的招牌场景，但它真实的复杂度不在命令，在**"Redis 和 DB 谁是主"以及"未登录态合并"**。

## 需求（真实电商购物车的功能清单）

| 功能 | 操作形状 |
|------|---------|
| 加购 / 改数量（+1 -1 直接改） | Hash field 写入 / HINCRBY |
| 删除商品 | HDEL |
| 打开购物车页（列表+角标数量） | HGETALL / HLEN |
| 勾选哪些商品参与结算 | 需要第二份"选中状态" |
| 登录后合并"未登录浏览时的购物车" | 两个 Hash 合并 |
| 商品下架/失效标灰 | 消费商品状态，购物车只存"有什么" |

## 数据设计

```bash
# 数量：用户ID → {skuId: 数量}
HSET cart:1001 sku1 2 sku2 1

# 选中状态：结算勾选（1=选中）
HSET cart:checked:1001 sku1 1

# 角标数量 = HLEN cart:1001（商品种类数，不是总件数）
```

**为什么不用 String 存 JSON**：加购/改数量是**单字段高频写**——JSON 要"读全量→改→写回全量"，并发下互相覆盖；Hash 的 `HINCRBY` 单字段原子自增，天然无竞态。

## 核心代码

```java
public void addItem(long userId, String skuId, int count) {
    // 校验商品可售（DB/商品服务）略
    redis.opsForHash().increment("cart:" + userId, skuId, count);
    asyncSaveToDb(userId, skuId);          // 双写：见下文"谁是主"
}

public Map<String, Long> view(long userId) {
    Map<Object, Object> cart = redis.opsForHash().entries("cart:" + userId);
    // 转换 + 合并商品服务信息（价格/标题/上下架状态）→ 购物车页只存ID和数量，
    // 商品详情实时从商品服务拿（否则价格改动购物车显示旧价）
}
```

**为什么购物车里不存价格**：价格是商品服务的属性，购物车页展示时实时取——否则商家改价后购物车显示旧价，结算页又是新价，用户体验灾难。（结算页锁定价格用"结算单"概念，另一个话题。）

## 未登录态合并（真实产品必有）

游客把商品放"本地购物车"（Cookie/localStorage），登录后合并到 Redis：

```java
public void mergeOnLogin(long userId, Map<String, Integer> localCart) {
    localCart.forEach((skuId, count) -> {
        Long existing = (Long) redis.opsForHash().get("cart:" + userId, skuId);
        // 相同商品：数量取大者（防登录前 Redis 已有记录被本地覆盖成 0）
        redis.opsForHash().put("cart:" + userId, skuId,
                Math.max(existing == null ? 0 : existing, count));
    });
}
```

## 谁是主存：Redis 还是 DB？（经典架构讨论）

| 方案 | 优点 | 风险 |
|------|------|------|
| **Redis 主存 + 异步落 DB**（主流） | 全程内存操作，丝滑 | Redis 挂/淘汰 → 购物车丢失（用户重新加购，可容忍） |
| DB 主存 + Redis 缓存 | 数据最稳 | 每次加购写 DB，高峰压力大；读走缓存有延迟 |

购物车是"丢失可容忍、体验要丝滑"的数据——**Redis 主存方案胜出**，同时异步落 DB 做**兜底与数据分析**（推荐系统想知道你加购过什么）。这与 stage3/01 的"分级可靠性"一致：订单不能丢所以 DB 主，购物车可容忍所以 Redis 主。

## 大 Key 防护

- 单用户购物车 > 50 种商品：前端提示"购物车已满，请先结算"（产品设计消灭大 Key）
- 防御性上限：`HLEN` 检查 + 超 200 拒绝加购
- 不用担心：单个 Hash ≤128 field 时是紧凑编码（stage2/02），购物车天然很小

## 结算时的闭坑

结算页"去结算"要生成**结算单快照**（锁定价格、数量），而不是直接读购物车结算——用户结算中途商家改价、商品下架，购物车数据会变。快照写入 DB，支付以快照为准。**购物车是"意向"，结算单才是"交易"。**

## 面试问法

**Q: 购物车为什么用 Hash 不用 String 存 JSON？**
A: 加购/改数量是单字段高频写。JSON 方案要读全量改后写回，并发下互相覆盖且浪费；Hash 的 HINCRBY 单字段原子自增天然安全，HDEL 单删，HGETALL 渲染。同时购物车选 Redis 主存 + 异步落 DB：丢失可容忍（用户重新加购）换全程内存丝滑；登录时本地购物车按"同商品数量取大"合并；价格不进购物车，结算用结算单快照。

---

**下一篇**：[03-List-Feed流时间线](03-List-Feed流时间线.md)——大厂高频的推拉模式之争。
