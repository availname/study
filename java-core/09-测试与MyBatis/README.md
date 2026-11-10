# 09 · 测试与 MyBatis（企业必备补课）

两个"企业天天用、教程很少教"的模块：

| 篇 | 主题 | 核心考点 |
|----|------|---------|
| [01](01-JUnit5与Mockito单元测试.md) | JUnit5 + Mockito 单元测试 | 断言/生命周期/Mockito mock 打桩/参数化测试/测试金字塔 |
| [02](02-MyBatis快速上手.md) | MyBatis(-Plus) | 注解与 XML CRUD / #{} vs ${}（SQL 注入）/ 分页插件 / 与 JdbcTemplate 对照 |
| [03](03-单元测试实战复盘.md) | **实战复盘** | 给 learn-center(17)+community(11) 补测试的 5 个真实坑 / mock 模板三件套（SCAN 游标/具体类/Map 消费者）/ 单测 vs 集成分工 |

## 为什么补这两个

- **测试**：stage6/07 讲了 Testcontainers 集成测试，但单元测试（JUnit5 断言/Mockito 隔离依赖）没系统讲——测试金字塔的 70% 靠它
- **MyBatis**：你项目用 JdbcTemplate（手写 SQL 映射），企业主流是 MyBatis(-Plus)——换工作第一周就要用的东西

## Testcontainers 集成测试（已落地）

community 的 [CommunityIntegrationTest](../../community/src/test/java/com/example/community/CommunityIntegrationTest.java) 是 stage6/07 的落地：真实 MySQL/Redis 容器跑 3 个回归用例（SETBIT 语义/消息表生命周期/幂等两层），`disabledWithoutDocker` 无 Docker 自动跳过。

**单元测试也已落地**（03 篇的素材）：learn-center `mvn test` 17 例、community 11 例——两个项目 README 各有"用例 ↔ 教学章节"映射表。
