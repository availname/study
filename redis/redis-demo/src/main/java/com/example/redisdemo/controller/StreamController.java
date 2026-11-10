package com.example.redisdemo.controller;

import com.example.redisdemo.common.ApiResponse;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * stage4/03：Redis Stream 队列（XADD/XREADGROUP/ACK + 后台消费者组）。
 * 对比 List：消息流内保留、消费者组分摊、ACK 确认、pending 可重投。
 */
@RestController
@RequestMapping("/stream")
public class StreamController {

    public static final String KEY = "demo:orders";
    public static final String GROUP = "order-consumers";
    public static final String CONSUMER = "consumer-" + (int) (Math.random() * 100);

    private final StringRedisTemplate redis;

    public StreamController(StringRedisTemplate redis) {
        this.redis = redis;
        ensureGroupAndConsumer();
    }

    private void ensureGroupAndConsumer() {
        StreamOperations<String, Object, Object> ops = redis.opsForStream();
        try {
            ops.createGroup(KEY, ReadOffset.from("0"), GROUP);   // 组从流头开始
        } catch (Exception e) {
            // BUSYGROUP：组已存在，正常
        }
        // 后台消费者：XREADGROUP 阻塞读取 → 处理 → ACK
        Executors.newSingleThreadExecutor().submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    List<MapRecord<String, Object, Object>> records = ops.read(
                            Consumer.from(GROUP, CONSUMER),
                            StreamReadOptions.empty().count(10).block(java.time.Duration.ofSeconds(5)),
                            StreamOffset.create(KEY, ReadOffset.lastConsumed()));
                    if (records == null) continue;
                    for (MapRecord<String, Object, Object> r : records) {
                        System.out.println("[Stream消费] " + r.getId() + " -> " + r.getValue()
                                + " (consumer=" + CONSUMER + ")");
                        ops.acknowledge(KEY, GROUP, r.getId());   // 处理完必须 ACK
                    }
                } catch (Exception e) {
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { return; }
                }
            }
        });
    }

    /** 生产消息：XADD（* 自动生成 时间戳-序号 ID） */
    @PostMapping("/send")
    public ApiResponse<String> send(@RequestParam(defaultValue = "1001") String orderNo) {
        RecordId id = redis.opsForStream().add(KEY, Map.of("orderNo", orderNo, "action", "创建"));
        return ApiResponse.ok("已入流, 消息ID=" + id);
    }

    /** 当前流长度 */
    @GetMapping("/len")
    public ApiResponse<Long> len() {
        return ApiResponse.ok(redis.opsForStream().size(KEY));
    }

    /** pending（已读未 ACK）消息数——消费者崩溃时会看到数字增长 */
    @GetMapping("/pending")
    public ApiResponse<Object> pending() {
        PendingMessages pm = redis.opsForStream()
                .pending(KEY, GROUP, org.springframework.data.domain.Range.unbounded(), 100);
        return ApiResponse.ok("pending=" + pm.size() + ", 本实例consumer=" + CONSUMER);
    }
}
