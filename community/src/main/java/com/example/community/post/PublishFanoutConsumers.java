package com.example.community.post;

import com.example.community.messaging.MessageDedupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 发布完成 fanout 的下游消费者（mq 教学 stage5/03 的实战版）：
 * ① Feed 推送：写每个粉丝的收件箱（推模式，LPUSH+LTRIM 防大 Key）
 * ② 作者通知：Redis List 通知收件箱
 * ③ 死信兜底：管道里任何重试耗尽的消息落库可见
 */
@Component
public class PublishFanoutConsumers {
    private static final Logger log = LoggerFactory.getLogger(PublishFanoutConsumers.class);
    private static final int MAX_INBOX = 200;   // 收件箱上限，防大 Key

    private final StringRedisTemplate redis;
    private final MessageDedupService dedup;
    private final NamedParameterJdbcTemplate jdbc;

    public PublishFanoutConsumers(StringRedisTemplate redis, MessageDedupService dedup,
                                  NamedParameterJdbcTemplate jdbc) {
        this.redis = redis;
        this.dedup = dedup;
        this.jdbc = jdbc;
    }

    /** ① Feed 推送（写扩散）：读作者的粉丝 Set → 每人收件箱 LPUSH + LTRIM */
    @RabbitListener(queues = "community.post.feed.queue")
    public void onFeed(Map<String, Object> msg) {
        long postId = ((Number) msg.get("postId")).longValue();
        long authorId = ((Number) msg.get("authorId")).longValue();
        String title = String.valueOf(msg.get("title"));

        String messageId = "post-published-" + postId;
        if (!dedup.tryConsume("feed-push", messageId)) return;

        Set<String> fans = redis.opsForSet().members("community:fans:" + authorId);
        int pushed = 0;
        if (fans != null) {
            for (String fan : fans) {
                redis.opsForList().leftPush("community:inbox:" + fan, String.valueOf(postId));
                redis.opsForList().trim("community:inbox:" + fan, 0, MAX_INBOX - 1);
                pushed++;
            }
        }
        log.info("[Feed推送] 《{}》({}) 推给 {} 个粉丝的收件箱", title, postId, pushed);
        // 生产注记：粉丝百万时这里会写放大 → 只推"活跃粉丝"，其余拉模式（stage5/03 推拉结合）
    }

    /** ② 作者通知：审核通过已发布 */
    @RabbitListener(queues = "community.post.notify.queue")
    public void onNotify(Map<String, Object> msg) {
        long authorId = ((Number) msg.get("authorId")).longValue();
        redis.opsForList().leftPush("community:notif:" + authorId,
                "你的帖子《" + msg.get("title") + "》已发布");
        redis.opsForList().trim("community:notif:" + authorId, 0, 49);
    }

    /** ③ 死信兜底：管道失败可见可控（绝不无声丢弃） */
    @RabbitListener(queues = "community.dlq.queue")
    public void onDeadLetter(Message message) {
        var props = message.getMessageProperties();
        String reason = "unknown";
        Object xDeath = props.getHeaders().get("x-death");
        if (xDeath instanceof List<?> deaths && !deaths.isEmpty() && deaths.get(0) instanceof Map<?, ?> d) {
            Object r = d.get("reason");
            if (r != null) reason = r.toString();
        }
        log.error("[死信] messageId={} reason={} body={}",
                props.getMessageId(), reason, new String(message.getBody(), StandardCharsets.UTF_8));
        jdbc.update("""
                INSERT INTO t_mq_dead_letter (message_id, source_queue, reason, payload)
                VALUES (:mid, :q, :r, :p)
                """, new MapSqlParameterSource()
                .addValue("mid", props.getMessageId())
                .addValue("q", props.getConsumerQueue())
                .addValue("r", reason)
                .addValue("p", new String(message.getBody(), StandardCharsets.UTF_8)));
    }
}
