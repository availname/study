package com.bigcenter.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 发送补偿任务（mq-mall SendRetryJob 总装版，stage3/01 本地消息表的"定时补偿"半边）：
 * 扫描超时未确认的消息 → 重发（复用同一 messageId）→ 指数退避 → 超限标记死信。
 * 与 Confirm 回调双通道：谁先到谁生效，幂等不冲突。
 */
@Component
public class SendRetryJob {
    private static final Logger log = LoggerFactory.getLogger(SendRetryJob.class);
    private static final int MAX_RETRY = 10;
    private static final long BASE_BACKOFF_MS = 30_000;

    private final SendRecordDao dao;
    private final ReliableMessagePublisher publisher;

    public SendRetryJob(SendRecordDao dao, ReliableMessagePublisher publisher) {
        this.dao = dao;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelay = 10_000)
    public void compensate() {
        List<Map<String, Object>> pending = dao.findPendingDue(100);
        for (Map<String, Object> row : pending) {
            String messageId = (String) row.get("message_id");
            int retryCount = ((Number) row.get("retry_count")).intValue();

            if (retryCount >= MAX_RETRY) {
                dao.markDead(messageId, "补偿重试超限");
                log.error("[补偿] messageId={} 重试超限标记死信，需人工处理", messageId);
                continue;
            }
            // 复用原始 payload 与 messageId 重发（幂等键不变，下游去重兜底）
            publisher.resend((String) row.get("exchange"),
                    (String) row.get("routing_key"),
                    messageId,
                    (String) row.get("payload"));
            long backoffMs = BASE_BACKOFF_MS << Math.min(retryCount, 5);   // 30s/1m/2m/4m/8m/16m
            dao.bumpRetry(messageId, retryCount + 1, Instant.now().plusMillis(backoffMs));
            log.warn("[补偿] 重发 messageId={} 第{}次", messageId, retryCount + 1);
        }
    }
}
