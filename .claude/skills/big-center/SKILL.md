---
name: big-center
description: 执行 big-center 总装项目——把 study 教学体系全部知识点融合成一个完整大项目（开发者成长平台）。当用户要求"按 big-center-PLAN 执行"、"继续 big-center"、"做总装项目"或新会话开始实现 big-center 时使用。内含提交约定、技术规格、10 步实施顺序、测试踩坑沉淀。
---

# big-center：全知识点总装项目

按 [big-center-PLAN.md](../../big-center-PLAN.md) 执行。该任务书是唯一事实源（功能-知识点对照表、包结构、技术规格都在里面），本 skill 内化的是**执行时的纪律与踩坑**。

## 项目一句话

`D:\JavaSpace\study\big-center` —— 开发者成长平台（端口 8085），9 个新功能（短链/扫码登录/限流/点赞/抢红包/推送/JWT/分布式ID/设计模式实战）+ 移植 4 个旧项目能力 + java-core-lab 实验室。

## 硬性纪律（每次提交前过一遍）

1. **提交人必须是 clz <2486681786@qq.com>**，提交信息全中文，**绝不带 Co-Authored-By / Claude / Generated with 等任何尾注**——用户明令要求。
2. **不另开仓库**：big-center 是 study 单仓库的子目录（remote 已配好 SSH：`git@github.com:availname/study.git`，push 用 `git push origin main`）。
3. gh CLI 不在 PATH：用 `D:\tools\gh\bin\gh.exe`。
4. Windows Git Bash：没有 `python3`（用 `python`）；sed 处理 Windows 路径易翻车，优先用 Edit 工具；控制台中文可能乱码，验证输出写文件再读。

## 技术规格速记

| 项 | 值 |
|----|----|
| 栈 | Spring Boot 3.2 / Java 17 / MySQL 8 / Redis 7 / RabbitMQ |
| 端口 | 8085（8090/8081/8082/8084 已被占） |
| DB | 复用 mq-mall-mysql(3307)，建库 `big_center` |
| Redis | 复用 6380；MQ 复用 5673 |
| 包 | `com.bigcenter.{shortlink,auth,pay,redpack,like,push,rate,common,lab}` |
| 错误码 | 1xxx 用户 / 2xxx 短链 / 3xxx 红包 / 4xxx 支付 |

## 实施顺序（每步完成标准：编译过 + 模块测试绿 + README 小节更新 + 提交）

1. ✅ 骨架：pom/WebConfig/ApiResponse/Ids 雪花/错误码 → IdsTest 绿（commit 8ca9a75）
2. ✅ JWT 认证（19 篇）：双 token/黑名单/拦截器（14d0ff4）
3. ✅ 短链（12 篇）：发号+302 跳转+统计+布隆（20bfe8d）
4. ✅ 支付中心（15 篇）：门面+策略+状态机 CAS+回调幂等+对账（ac7c852）
5. ✅ 红包（14 篇）：二倍均值预拆/LPOP 认领/TTL+DLX 延迟退款（e49731e）
6. ✅ 点赞+推送（14 篇）：Lua 原子+MQ 刷库+SSE（0459037）
7. ✅ 扫码登录（12 篇）：uuid 状态机+轮询（a1e2f6c）
8. ✅ 限流器（12 篇）：Lua 滑动窗口+@RateLimit AOP 打到前面所有接口（00c5ac3）
9. ✅ java-core-lab：并发/基石/设计模式/JVM 的 main 示例集（cfa9eca）
10. ✅ 收尾：README 对号入座表 + 全量测试（0505e01）——66 例全绿
11. ✅ 总装能力补齐（B 部分三项）：seckill 秒杀四层漏斗 / post 审核管道+热榜衰减 / ai Mock-Real 双实现——79 例全绿
12. ✅ 可靠消息全家桶移植（mq-mall stage3）：messaging/ 四件套（本地消息表+Confirm+补偿+幂等两层+死信落库），点赞刷库/红包 TTL+DLX 延迟退款接入——86 例全绿

中途中断后恢复：`git log --oneline` 看最后完成的步骤，从下一步继续。

## 测试纪律（纯 Mockito，无 Docker 全绿——学习体系铁律）

学 learn-center/redis-demo 的写法（`study/learn-center/src/test`、`study/redis/redis-demo/src/test`）。已踩过的坑直接避开：

- **Long 装箱**：Map key 存 `1001L` 就必须 `get(1001L)`——`get(1001)` 永远 null（Integer≠Long）。所有 ID 字面量带 L。
- **RedisScript 匹配**：`when(redis.execute(Mockito.<RedisScript<Long>>any(), Mockito.<List<String>>any(), any()))`，不要 `any(DefaultRedisScript.class)`。
- **MyBatis-Plus LambdaQueryWrapper** 纯单测要先 `TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), 实体.class)`。
- **MP insert 两个重载**：verify 用 `ArgumentMatchers.<T>any()` 消歧。
- **stub 算 interaction**："无动作"断言用 `verify(x, never()).add(...)`，不要 `verifyNoInteractions`。
- **链式 mock**：先 `when(redis.opsForValue()).thenReturn(valueOps)` 固定返回同一实例；`delete` 在 template 上不在 ValueOperations 上。**每个用到的 ops 都要固定**（opsForSet/opsForList 同理——漏了返回 null 抛 NPE）。
- **连续返回**：`thenReturn(1, 0)` 做重试/CAS 两段路径。
- **MP insert 回填自增 ID**：mock 里 `thenAnswer(inv -> { inv.getArgument(0, T.class).setId(1L); return 1; })`——thenReturn(1) 不回填，业务读 `getId()` 会 NPE。
- **JWT 测试确定性**：同毫秒同 payload 的 token 逐字节相同（无 jti）——"A 在黑名单、B 放行"类用例用不同 userId 区分。
- **RedisTemplate.delete 无双 String 重载**：多 key 用 `delete(List.of(k1, k2))`。
- **J17 注意**：`newVirtualThreadPerTaskExecutor`、`Thread.sleep(Duration)`、`ExecutorService implements AutoCloseable` 都是 19/21 的 API，J17 用不上。
- **Java 类名=文件名**：一个文件只放一个 public 类（TraceIdFilter 写进 TraceIdConfig.java 直接编译错）。

每个模块 ≥3 个用例，重点钉：Lua 语义、状态机 CAS、幂等跳过。用例名用中文长名写场景（命名即文档）。

## 每步的 README 对号入座

big-center/README.md 维护一张"功能 ↔ 教学章节"表（这是本项目教学价值所在）：每实现一个功能，补一行，链接回 `../java-core/...` 对应篇目。规范篇（16 接口/17 日志）吃自己的狗粮：ApiResponse/MDC traceId/日志六要素贯穿所有模块。
