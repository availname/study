# 01 · JUnit5 与 Mockito：单元测试入门

测试金字塔（stage6/07）：单元测试占 70%——快（毫秒）、不依赖外部、锁定逻辑正确性。

## JUnit5 基础

```java
class IdsTest {

    @BeforeEach                          // 每个测试前执行（初始化）
    void setUp() { /* ... */ }

    @Test
    void 下单金额应为单价乘数量() {
        // 给定 Arrange → 当 Act → 断言 Assert（AAA 结构）
        int amount = 3 * 100;
        assertEquals(300, amount);                  // 相等断言
        assertTrue(amount > 0);                     // 布尔断言
        assertThrows(BizException.class,            // 异常断言
                () -> orderService.pay("不存在"));
    }

    @Test
    void 金额不足应抛出库存不足() { /* ... */ }

    @AfterEach void tearDown() { /* 清理 */ }

    @ParameterizedTest                               // 参数化：一组数据跑同一逻辑
    @ValueSource(ints = {0, -1, 1000000})
    void 非法数量应拒绝(int count) {
        assertThrows(BizException.class, () -> service.create(1, 1, count));
    }
}
```

**命名即文档**：测试方法名写中文业务语义（如上）——测试就是可执行的规格说明。

## Mockito：隔离依赖（只测"这一个类"）

被测类依赖别的类（DB/Redis/HTTP）→ **mock 掉依赖**，只验证本类的逻辑与调用行为：

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock StockDao stockDao;                 // 假的依赖
    @Mock ReliableMessagePublisher publisher;
    @InjectMocks OrderService orderService;  // 被测类（依赖自动注入 mock）

    @Test
    void 库存不足应抛异常且不发消息() {
        when(stockDao.lock(1L, 2)).thenReturn(0);          // 打桩：lock 返回 0

        assertThrows(BizException.class, () -> orderService.create(1L, 1L, 2));

        verify(publisher, never()).sendInTransaction(any(), any(), any(), any());  // 验证：没发消息
        verify(stockDao).lock(1L, 2);                       // 验证：确实查了库存
    }

    @Test
    void 支付成功应发两条消息() {
        when(stockDao.lock(any(), any())).thenReturn(1);
        when(orderDao.find(any())).thenReturn(Optional.of(new OrderDao.OrderRow(1L, "O1", 1L, 1L, 1, 100, 20)));
        // ... mock orderDao.find / insert / casPay 后执行 pay
        verify(publisher, times(2)).sendFast(any(), any(), any(), any());   // 支付事件×1（timeout 另发）
    }
}
```

**三个核心动词**：`when(...).thenReturn(...)`（打桩）、`verify(mock, times(n)).方法(...)`（验证调用）、`ArgumentCaptor`（捕获入参断言内容）。

## 测试金字塔与实战分配（stage6/07 补全）

| 层 | 占比 | 工具 | 测什么 |
|----|------|------|--------|
| 单元测试 70% | JUnit5 + Mockito | 纯逻辑：状态机判断/CAS 循环/参数校验/金额计算 |
| 集成测试 20% | Testcontainers | 幂等链路/消息表生命周期/SQL 真实行为（community 已有） |
| 端到端 10% | HTTP 冒烟 | 关键用户路径 |

**写什么的清单**：每次修 bug → 先写一个"能复现 bug 的测试"再修（回归锁定）；每个公共工具类 → 配套测试；**你踩过的 12 个坑 = 12 个待写用例**。

## 面试问法

**Q: 单元测试和集成测试的区别？你们怎么分配？**
A: 单元测试隔离依赖（Mockito mock），毫秒级测逻辑分支，占 70%；集成测试用 Testcontainers 起真容器测幂等/事务/序列化等"mock 测不出"的链路，占 20%；CI 全跑（stage6/07 的 disabledWithoutDocker 优雅降级）。修 bug 必配回归测试锁住。

---

**下一篇**：[02-MyBatis 快速上手](02-MyBatis快速上手.md)。
