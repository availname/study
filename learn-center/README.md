# learn-center：在线学习平台（终极整合项目）

> 🤖 **已融合 AI 能力**（stage7）：AI 答疑（SSE 流式 + 题目上下文 RAG）、AI 出题入库（结构化输出）、学习报告（数据聚合 + AI 表述）——`lc.ai.provider=mock` 默认无 Key 全流程可跑，配真实 Key（DeepSeek/通义兼容端点）即切换真模型。详见 `ai/` 包与 [stage7 教程](../stage7-AI应用开发/README.md)。

> **融合整个教学体系的毕业项目**：Java 并发/JVM/MySQL 深入/Redis 五结构/RabbitMQ 全链路/MyBatis-Plus/Spring 事务——每个业务功能都标注了对应的教学章节。用它来学它教的东西。

## 业务：在线学习平台

注册登录 → 浏览课程 → **限量抢课**（秒杀四层漏斗）→ 支付（CAS 防超时竞态）→ **每日打卡**（Bitmap）→ **学习时长榜**（ZSet）→ **刷题错题本** → **通知收件箱**。

## 技术栈（对比前三个项目的升级）

| 维度 | 学习项目 | 本项目 |
|------|---------|--------|
| ORM | JdbcTemplate 手写映射 | **MyBatis-Plus**（BaseMapper/Lambda 分页/条件构造器） |
| 鉴权 | 每接口传 token | **拦截器统一鉴权 + ThreadLocal UserContext**（含 afterCompletion 清理） |
| 事务/可靠性 | 已有模式 | 同款复刻（本地消息表/CAS/双保险） |

## 核心链路：限量抢课（融合度最高的一条链）

```
POST /api/enroll/{courseId}
  ① 每日限 3 门（String INCR+TTL Lua——stage5/01）
  ② Redis Lua 原子预扣：名额+一人一课 Set 判断登记（stage4/01）
  ③ MQ 异步建单（削峰）→ 消费者 INSERT 订单（唯一索引兜底幂等 stage3/02）
     + DB 名额扣减 WHERE enrolled<quota（防超卖兜底）
     + 发延迟取消消息（TTL+DLX 延迟链 stage3/05）
  → 用户支付：CAS 待支付→已支付（stage4/02 竞态互斥）
  → 超时消费者：CAS 取消 + Redis/DB 名额双回补
```

## 运行

```bash
# 前置：mq-mall 三件套容器在跑（MySQL 3307 / RabbitMQ 5673 / Redis 6380）
docker exec mq-mall-mysql mysql -uroot -proot123 -e "CREATE DATABASE IF NOT EXISTS learn_center DEFAULT CHARSET utf8mb4"
cd learn-center && mvn spring-boot:run    # 应用 @ localhost:8084，建表自动执行
```

## 单元测试（无 Docker 也能跑）

```bash
mvn test    # 17 个用例，纯 Mockito 单测，不依赖任何容器/中间件
```

| 测试类 | 固定住的行为 | 对应教学 |
|--------|------------|---------|
| `IdsTest` | 简化雪花同毫秒不撞号、严格递增（幂等防误杀的前提） | 踩坑实录坑二 |
| `AiQuestionGeneratorTest` | JSON 围栏剥离 / 标题去重 / 难度钳制 1~3 / 解析失败兜底 BizException | stage7/02 结构化输出、stage7/04 降级 |
| `EnrollConsumersTest` | 建单→扣名额→延迟取消消息；重投幂等跳过；CAS 取消失败不回补 | mq stage3 幂等、stage4 延迟链 |
| `LearningToolsTest` | Agent 三工具的组装/映射/缺省值兜底（工具只读、身份注入） | stage7/06 Agent Tool Calling |

**测试技巧沉淀**（09-测试与MyBatis 的实操版）：Mockito mock 具体类（JdbcTemplate/RedisTemplate）；MyBatis-Plus `LambdaQueryWrapper` 在纯单测中需 `TableInfoHelper.initTableInfo` 注册实体元数据；MP 3.5.7 的 `insert` 有 `T`/`Collection<T>` 两个重载，verify 时要用 `ArgumentMatchers.<T>any()` 消歧。

## API 路线

```bash
# 注册登录
curl -X POST "http://localhost:8084/api/auth/register?username=alice&password=1234"
TOKEN=$(curl -s -X POST "http://localhost:8084/api/auth/login?username=alice&password=1234" | 提取 token)

# 课程（缓存击穿防护演示：清缓存后并发打详情，只有一个请求回源 DB）
curl "http://localhost:8084/api/courses"
curl "http://localhost:8084/api/courses/1"

# 限量抢课（每课 30 名额，重复抢被一人一课拦截）
curl -X POST -H "Authorization: $TOKEN" "http://localhost:8084/api/enroll/1"

# 支付 / 我的订单
curl -X POST -H "Authorization: $TOKEN" "http://localhost:8084/api/enroll/orders/E123/pay"
curl -H "Authorization: $TOKEN" "http://localhost:8084/api/enroll/my"

# 学习打卡（Bitmap）/ 学习时长榜（ZSet）/ UV（HLL）
curl -X POST -H "Authorization: $TOKEN" "http://localhost:8084/api/study/sign"
curl -X POST -H "Authorization: $TOKEN" "http://localhost:8084/api/study/learn?minutes=30"
curl "http://localhost:8084/api/study/rank"
curl "http://localhost:8084/api/study/uv"

# 面试刷题（元闭环：题库=java-core 面试速查的题）
curl "http://localhost:8084/api/interview/daily"
curl -X POST -H "Authorization: $TOKEN" "http://localhost:8084/api/interview/1/wrong"
curl -H "Authorization: $TOKEN" "http://localhost:8084/api/interview/wrongbook"

# 通知 / 大盘
curl -H "Authorization: $TOKEN" "http://localhost:8084/api/notifications"
curl "http://localhost:8084/api/admin/stats"
```

## 知识融合映射（每个功能 = 哪些教学章节）

| 功能 | 融合的知识点 | 出处 |
|------|-------------|------|
| 登录态 | String+TTL（7 天）+ 拦截器 ThreadLocal 传递（afterCompletion 清理） | r-stage1/05 · 并发07 |
| 发帖/抢课防刷 | INCR+首次TTL Lua（原子限流） | r-stage5/01 |
| 限量抢课 | 分布式闸门（Redisson 看门狗锁，两参 tryLock 自动续期）→ Lua 原子预扣（名额+一人一课）→ MQ 削峰建单 → DB 兜底 | r-stage3/03 · m-stage4/01 |
| 订单超时取消 | TTL+DLX 延迟链 + CAS 取消 + 名额双回补 | m-stage4/02 |
| 课程详情缓存 | 互斥锁防击穿（SET NX 重建锁） | r-stage3/01 |
| 学习打卡 | Bitmap（SETBIT 幂等 + 连签位串回扫） | r-stage5/06 |
| 学习时长榜 | ZSet ZINCRBY + ZREVRANGE | r-stage5/02 |
| UV | HLL PFADD/PFCOUNT | r-stage5/07 |
| 消息可靠投递 | 本地消息表 + Confirm + 补偿 Job（复刻 mq-mall） | m-stage3/01 |
| 失败兜底 | 重试 3 次 → 死信落库 | m-stage3/05 |
| ORM | MyBatis-Plus BaseMapper/Lambda 分页（新引入） | j-core/09 |

## 教学简化点

| 简化 | 生产 |
|------|------|
| token=UUID+Redis | JWT 无状态 + 刷新令牌（**对比分析与升级地图：[java-core/19-登录态与接口安全](../java-core/19-登录态与接口安全.md)**） |
| MD5 密码 | BCrypt（同上 19 篇·第四节） |
| 抢课异步建单无本地消息表 | MQ 挂了单据丢失 → 补本地消息表（可从 community/mq-mall 复制 ReliableMessagePublisher 全套） |
| 看门狗锁是单机 Redis 锁 | 多节点部署换 RedLock 或按 r-stage3/05 上 fencing token 对账 |
| 单库 | 分库分表见 MySQL 06 篇 |
