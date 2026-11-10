package com.example.mqdemo.controller;

import com.example.mqdemo.seckill.SeckillMsg;
import com.example.mqdemo.seckill.SeckillService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 所有演示入口。启动后按 demo/README.md 的顺序逐个访问。
 */
@RestController
public class DemoController {

    private final RabbitTemplate rabbit;
    private final SeckillService seckillService;

    public DemoController(RabbitTemplate rabbit, SeckillService seckillService) {
        this.rabbit = rabbit;
        this.seckillService = seckillService;
    }

    // ============ stage1/03 Hello World ============
    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "你好，消息队列") String msg) {
        rabbit.convertAndSend("", "hello.queue", msg);
        return "已发送: " + msg;
    }

    // ============ stage1/04 Work Queues：发 10 个任务，观察两个消费者公平分发 ============
    @GetMapping("/work")
    public String work(@RequestParam(defaultValue = "10") int n) {
        for (int i = 1; i <= n; i++) {
            rabbit.convertAndSend("", "work.queue", "任务-" + i);
        }
        return "已发 " + n + " 个任务到 work.queue";
    }

    // ============ stage1/04 Topic：换 key 试试谁收到 ============
    // key=order.create     → order.* 和 order.# 都收
    // key=order.pay.success→ 只有 order.# 收（* 只匹配一个词）
    // key=user.register    → 谁都不收 → 触发 Return 回调！（stage2/03）
    @GetMapping("/topic")
    public String topic(@RequestParam(defaultValue = "order.create") String key) {
        rabbit.convertAndSend("biz.topic", key, "事件: " + key);
        return "已发 key=" + key + "（若无队列匹配，看控制台 [Return] 日志）";
    }

    // ============ stage3/02 幂等：10 条相同 messageId，业务只执行 1 次 ============
    @GetMapping("/idempotent")
    public String idempotent() {
        String messageId = "order-create-" + UUID.randomUUID();
        for (int i = 0; i < 10; i++) {
            rabbit.convertAndSend("", "idempotent.queue",
                    Map.of("messageId", messageId, "amount", 9900));
        }
        return "已发 10 条相同 messageId=" + messageId + " 的消息，观察消费者日志";
    }

    // ============ stage3/05 死信：消费失败重试 3 次 → 进 demo.dlq ============
    @GetMapping("/dlq")
    public String dlq() {
        rabbit.convertAndSend("", "demo.biz.queue", "boom-" + System.currentTimeMillis());
        return "已发一条必失败消息，等待重试耗尽后进 demo.dlq（约 3~4 秒）";
    }

    // ============ stage3/05 延迟：5 秒后才被消费（TTL+DLX 演示版） ============
    @GetMapping("/delay")
    public String delay() {
        rabbit.convertAndSend("", "order.delay.queue", "延迟订单-" + System.currentTimeMillis());
        return "已发延迟消息：5 秒后才会在 order.close.queue 被消费";
    }

    // ============ stage4/01 秒杀 ============
    /** 模拟并发抢购：count 个请求瞬间打进来 */
    @PostMapping("/seckill")
    public Map<String, Object> seckill(@RequestParam(defaultValue = "1000") int count) {
        return seckillService.seckill(count);
    }

    @GetMapping("/seckill/stats")
    public Map<String, Object> seckillStats() {
        return seckillService.stats();
    }
}
