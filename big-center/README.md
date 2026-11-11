# big-center —— 开发者成长平台

学习体系的**总装厂**：把 java-core 21 篇 + mq/redis 四阶段 + 4 个旧项目的知识点，总装进一个真实业务（任务书见 [`../big-center-PLAN.md`](../big-center-PLAN.md)）。

```
big-center (8085)
├── 门户/短链服务      短链接系统（12 篇）
├── 用户服务          JWT 双 token 登录（19 篇）+ 扫码登录（12 篇）
├── 学习/社区业务      点赞（14-1）/ 消息推送（14-3）
├── 活动运营          限时红包雨（14-2）
├── 支付中心          模拟支付网关（15 篇）
├── 基础设施          限流器（12-3）/ 分布式 ID（11）/ 接口规范（16）/ 日志规范（17）
└── java-core-lab     并发/基石/设计模式可运行 main 示例集
```

## 架构

> TODO：收尾阶段补 Mermaid 架构图

## 功能 ↔ 教学章节 对号入座

| 功能 | 教学出处 | 状态 |
|------|---------|------|
| 分布式 ID（雪花，时钟回拨处理） | [java-core/11-分布式ID](../java-core/) | ✅ `common/Ids` |
| 接口规范（ApiResponse/错误码分段） | [java-core/16-接口设计](../java-core/) | ✅ `common/` |
| 日志规范（MDC traceId/六要素） | [java-core/17-日志规范](../java-core/) | ✅ `config/TraceIdFilter` |
| JWT 登录（双 token/黑名单） | java-core/19 | ⬜ |
| 短链接服务（发号/302/布隆） | java-core/12 题一 | ⬜ |
| 支付中心（状态机 CAS/回调幂等） | java-core/15 | ⬜ |
| 抢红包（二倍均值/TTL+DLX） | java-core/14 题二 | ⬜ |
| 点赞系统（Lua 原子/MQ 刷库） | java-core/14 题一 | ⬜ |
| 消息推送（SSE+离线落库） | java-core/14 题三 | ⬜ |
| 扫码登录（uuid 状态机） | java-core/12 题二 | ⬜ |
| 限流器（Lua 滑动窗口+AOP） | java-core/12 题三 | ⬜ |
| 设计模式实战（责任链/策略/状态机/门面） | java-core/06 | ⬜ |
| java-core-lab 实验室 | java-core/01-05/06/08 | ⬜ |

## 包结构

```
com.bigcenter
├── common      ApiResponse/ErrorCode/Ids/全局异常（规范地基，16/17 篇吃狗粮）
├── config      WebConfig/TraceIdFilter（第 2 步起接入 JWT 拦截器）
├── auth        JWT 认证（第 2 步）
├── shortlink   短链（第 3 步）
├── pay         支付中心（第 4 步）
├── redpack     红包（第 5 步）
├── like/push   点赞+推送（第 6 步）
├── rate        限流器（第 8 步）
└── lab         java-core-lab（第 9 步）
```

## 运行

```bash
# 依赖：mq-mall-mysql(3307)/mq-mall-redis(6380)/mq-mall-rabbitmq(5673) 已有 compose
docker exec mq-mall-mysql mysql -uroot -proot123 -e "CREATE DATABASE IF NOT EXISTS big_center DEFAULT CHARSET utf8mb4"
mvn spring-boot:run
```

## 测试

```bash
mvn test    # 纯 Mockito 单测，无 Docker 全绿（学习体系铁律）
```
