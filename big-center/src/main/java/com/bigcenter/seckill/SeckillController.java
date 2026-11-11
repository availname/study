package com.bigcenter.seckill;

import com.bigcenter.auth.UserContext;
import com.bigcenter.common.ApiResponse;
import com.bigcenter.common.Ids;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 限量课秒杀 = 秒杀四层漏斗（learn-center EnrollController 模式总装，mq-mall 秒杀篇依据）：
 * ① 分布式闸门（此处教学简化为 @RateLimit 接口级限流，多实例闸门见 learn-center Redisson 版）
 * ② Lua 原子预扣（名额+一人一课，lua/seckill.lua）
 * ③ MQ 异步建单（削峰：洪峰进队列，消费者匀速落库）
 * ④ DB 唯一约束兜底（fencing token 思想——正确性最终防线）
 */
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    static final String QUOTA_PREFIX = "bc:seckill:quota:";
    static final String USERS_PREFIX = "bc:seckill:users:";

    private final StringRedisTemplate redis;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbit;
    private final DefaultRedisScript<Long> seckillScript;

    public SeckillController(StringRedisTemplate redis,
                             org.springframework.amqp.rabbit.core.RabbitTemplate rabbit) throws Exception {
        this.redis = redis;
        this.rabbit = rabbit;
        this.seckillScript = new DefaultRedisScript<>();
        this.seckillScript.setResultType(Long.class);
        this.seckillScript.setScriptText(new String(
                new ClassPathResource("lua/seckill.lua").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8));
    }

    /**
     * 抢限量课：Lua 原子预扣 → 预扣成功发 MQ 异步建单（削峰）。
     * 第一层限流由 @RateLimit AOP 打在这类热点接口上（rate/ 模块）。
     */
    @PostMapping("/{courseId}")
    public ApiResponse<Map<String, Object>> grab(@PathVariable long courseId) {
        Long userId = UserContext.get();
        Long r = redis.execute(seckillScript,
                List.of(QUOTA_PREFIX + courseId, USERS_PREFIX + courseId),
                String.valueOf(userId));
        int code = r == null ? -1 : r.intValue();
        Map<String, Object> view = new LinkedHashMap<>();
        if (code == 1) {
            String orderNo = "S" + Ids.nextId();
            // 预扣成功 → MQ 异步建单（消费者 SeckillConsumer 匀速落库，DB 唯一索引兜底幂等）
            rabbit.convertAndSend("bc.seckill", "seckill.created",
                    Map.of("orderNo", orderNo, "userId", userId, "courseId", courseId));
            view.put("orderNo", orderNo);
            view.put("状态", "排队中，稍后查 /api/seckill/result/" + courseId);
            return ApiResponse.ok(view);
        }
        if (code == -2) return ApiResponse.error(4005, "你已抢过这门课（一人一课）");
        return ApiResponse.error(4006, "名额已满");
    }

    /** 抢购结果轮询（漏斗第四层：前端轮询拿最终结果） */
    @GetMapping("/result/{courseId}")
    public ApiResponse<Map<String, Object>> result(@PathVariable long courseId) {
        long userId = UserContext.get();
        Boolean joined = redis.opsForSet().isMember(USERS_PREFIX + courseId, String.valueOf(userId));
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("已抢到名额", Boolean.TRUE.equals(joined));
        return ApiResponse.ok(view);
    }
}
