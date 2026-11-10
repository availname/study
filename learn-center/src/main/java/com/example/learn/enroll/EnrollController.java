package com.example.learn.enroll;

import com.example.learn.common.ApiResponse;
import com.example.learn.common.BizException;
import com.example.learn.common.Ids;
import com.example.learn.user.UserContext;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 抢课接口 = 限量课秒杀（mq-mall 秒杀四层漏斗的模式复刻）：
 * 分布式闸门（Redisson 看门狗锁，多实例洪峰排队+快速失败）→ Lua 原子预扣（名额+一人一课）
 * → MQ 异步建单 → 前端轮询结果；正确性最终靠 DB 唯一约束兜底（fencing token 思想）。
 */
@RestController
@RequestMapping("/api/enroll")
public class EnrollController {

    /** Lua：判断(已购/售罄) + 扣名额 + 登记，原子执行（lua/enroll.lua） */
    private final DefaultRedisScript<Long> enrollScript;
    private final StringRedisTemplate redis;
    private final RabbitTemplate rabbit;
    private final RedissonClient redisson;

    public EnrollController(StringRedisTemplate redis, RabbitTemplate rabbit,
                            RedissonClient redisson) throws Exception {
        this.redis = redis;
        this.rabbit = rabbit;
        this.redisson = redisson;
        this.enrollScript = new DefaultRedisScript<>();
        this.enrollScript.setResultType(Long.class);
        this.enrollScript.setScriptText(new String(
                new ClassPathResource("lua/enroll.lua").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8));
    }

    /**
     * 抢限量课：分布式闸门 → Lua 原子预扣 → 发 MQ 异步建单（削峰）。
     * 锁的意义（r-stage3/03）：Lua 只保证 Redis 操作原子，多实例部署时同一课程的
     * 洪峰需要一把跨实例闸门串行排队+快速失败，防止瞬时洪峰全部打到 Redis/Lua。
     * 两参 tryLock 不传 leaseTime → 看门狗启用（默认 30s，每 10s 续回），业务慢不怕锁先过期。
     */
    @PostMapping("/{courseId}")
    public ApiResponse<Map<String, Object>> enroll(@RequestHeader("Authorization") String token,
                                                   @PathVariable long courseId) {
        long userId = UserContext.get();
        RLock lock = redisson.getLock("lc:lock:enroll:" + courseId);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(3, TimeUnit.SECONDS);
            if (!acquired) return ApiResponse.error("抢课人数过多，请稍后再试");
            return doEnroll(userId, courseId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ApiResponse.error("抢课被中断，请重试");
        } finally {
            // 防误删他人锁：只解自己当前持有的（对应 SETNX 演进里的 requestId 校验）
            if (acquired && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    private ApiResponse<Map<String, Object>> doEnroll(long userId, long courseId) {
        Long r = redis.execute(enrollScript,
                List.of("lc:enroll:quota:" + courseId, "lc:enroll:users:" + courseId),
                String.valueOf(userId));
        int code = r == null ? -1 : r.intValue();
        Map<String, Object> view = new LinkedHashMap<>();
        if (code == 1) {
            String orderNo = "E" + Ids.nextId();
            // 预扣成功 → MQ 异步建单（削峰：洪峰进队列，消费者匀速落库）
            rabbit.convertAndSend("lc.enroll", "enroll.created",
                    Map.of("orderNo", orderNo, "userId", userId, "courseId", courseId));
            view.put("orderNo", orderNo);
            view.put("状态", "排队中，稍后查 /api/enroll/my");
            return ApiResponse.ok(view);
        }
        if (code == -2) return ApiResponse.error("你已抢过这门课（一人一课）");
        return ApiResponse.error("名额已满");
    }

    /** 我的抢课结果轮询 */
    @GetMapping("/result/{courseId}")
    public ApiResponse<Map<String, Object>> result(@RequestHeader("Authorization") String token,
                                                   @PathVariable long courseId) {
        long userId = com.example.learn.user.UserContext.get();
        Boolean joined = redis.opsForSet().isMember("lc:enroll:users:" + courseId, String.valueOf(userId));
        Long quota = redis.opsForValue().get("lc:enroll:quota:" + courseId) == null ? null
                : Long.parseLong(java.util.Objects.requireNonNull(
                        redis.opsForValue().get("lc:enroll:quota:" + courseId)));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("已抢到名额", Boolean.TRUE.equals(joined));
        view.put("剩余名额(Redis)", quota);
        return ApiResponse.ok(view);
    }
}
