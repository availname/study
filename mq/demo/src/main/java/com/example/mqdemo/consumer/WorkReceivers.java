package com.example.mqdemo.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * stage1/04 Work Queues：两个消费者竞争 work.queue。
 * 快消费者 vs 慢消费者（sleep 模拟），配合 prefetch=1 的容器工厂观察"能者多劳"。
 */
@Component
public class WorkReceivers {
    private static final Logger log = LoggerFactory.getLogger(WorkReceivers.class);

    @RabbitListener(queues = "work.queue", containerFactory = "workFactory")
    public void fast(String task) {
        log.info("[Work-快] 处理完成: {}", task);
    }

    @RabbitListener(queues = "work.queue", containerFactory = "workFactory")
    public void slow(String task) throws InterruptedException {
        Thread.sleep(800);   // 模拟慢任务
        log.info("[Work-慢] 处理完成: {}", task);
    }
}
