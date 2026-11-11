package com.example.redisdemo.seckill;

import com.example.redisdemo.controller.SeckillController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 秒杀 Lua 语义单测（stage4/01）——不连 Redis，用 Mock 模拟 Redis 的键值/集合行为，
 * 把"Lua 脚本的判定逻辑"作为被测对象：超卖拦截 / 一人一单 / 售罄 / 异常兜底。
 * 真实原子性由 Redis 单线程保证（教学演示接口压测验证），单测固定的是"决策语义"。
 */
class SeckillLogicTest {

    private StringRedisTemplate redis;
    /** 模拟 GET stock 的返回（null=未初始化；用 AtomicInteger 模拟 DECR 语义） */
    private final AtomicInteger stock = new AtomicInteger(-1);
    /** 模拟 SISMEMBER：抢到过的用户集合 */
    private final Map<Long, Boolean> users = new ConcurrentHashMap<>();
    private SeckillController controller;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.opsForSet()).thenReturn(setOps);

        // 模拟 Redis 行为：GET stock → 当前库存；execute(SECKILL) → 按脚本语义用 Java 等价复现
        when(valueOps.get("seckill:stock:1")).thenAnswer(inv -> stock.get() < 0 ? null : String.valueOf(stock.get()));
        when(redis.execute(Mockito.<RedisScript<Long>>any(), Mockito.<List<String>>any(), any()))
                .thenAnswer(inv -> {
                    String userId = (String) inv.getArguments()[2];
                    long uid = Long.parseLong(userId);
                    // 脚本第一行：SISMEMBER users uid == 1 → return -2（一人一单）
                    if (users.containsKey(uid)) return -2L;
                    // 第二行：stock = tonumber(GET or '-1')；<=0 → return 0（售罄）
                    int cur = stock.get();
                    if (cur <= 0) return 0L;
                    // DECR + SADD → return 1
                    stock.decrementAndGet();
                    users.put(uid, true);
                    return 1L;
                });

        controller = new SeckillController(redis);
    }

    void resetStock(int n) {
        stock.set(n);
        users.clear();
        SeckillController.ORDERS.clear();
    }

    @Test
    @DisplayName("正常抢购：扣库存并记订单")
    void seckillSuccess() {
        resetStock(10);
        assertThat(controller.seckill(1001).code()).isEqualTo(0);
        assertThat(stock.get()).isEqualTo(9);
        assertThat(SeckillController.ORDERS.get(1001L)).isEqualTo("已抢到");
    }

    @Test
    @DisplayName("售罄拦截：库存 0 返回错误且不再扣减")
    void seckillSoldOut() {
        resetStock(0);
        assertThat(controller.seckill(1001).code()).isNotEqualTo(0);
        assertThat(stock.get()).isLessThanOrEqualTo(0);
        assertThat(SeckillController.ORDERS).doesNotContainKey(1001L);
    }

    @Test
    @DisplayName("库存未初始化：数据异常兜底，不产生订单")
    void seckillUninitialized() {
        resetStock(5);
        stock.set(-1);   // 模拟没 reset 就抢
        assertThat(controller.seckill(1001).code()).isNotEqualTo(0);
        assertThat(SeckillController.ORDERS).doesNotContainKey(1001L);
    }

    @Test
    @DisplayName("库存 1 时两人并发抢：恰好一人成功（不超卖）")
    void noOversell() {
        resetStock(1);
        // 串行模拟"并发"：Lua 在 Redis 单线程下本来就是这样逐条执行的
        var r1 = controller.seckill(2001);
        var r2 = controller.seckill(2002);
        assertThat(r1.code()).isEqualTo(0);
        assertThat(r2.code()).isNotEqualTo(0);
        assertThat(stock.get()).isEqualTo(0);          // 只扣了 1 次
        assertThat(users).hasSize(1);                  // 只登记 1 人
    }

    @Test
    @DisplayName("重复抢购：第二个请求被一人一单拦截")
    void duplicateUserRejected() {
        resetStock(10);
        assertThat(controller.seckill(3001).code()).isEqualTo(0);
        var again = controller.seckill(3001);
        assertThat(again.code()).isNotEqualTo(0);
        assertThat(stock.get()).isEqualTo(9);          // 没有二次扣减
    }

    @Test
    @DisplayName("连抢 5 人 / 库存 3：恰好 3 人成功 2 人售罄")
    void exactBoundary() {
        resetStock(3);
        int ok = 0, fail = 0;
        for (long uid = 4001; uid <= 4005; uid++) {
            if (controller.seckill(uid).code() == 0) ok++; else fail++;
        }
        assertThat(ok).isEqualTo(3);
        assertThat(fail).isEqualTo(2);
        assertThat(stock.get()).isZero();
    }
}
