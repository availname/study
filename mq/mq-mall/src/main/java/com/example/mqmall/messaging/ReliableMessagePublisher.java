package com.example.mqmall.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;

/**
 * 可靠消息发送门面（教程 stage3/01 本地消息表的生产级封装）。
 *
 * sendInTransaction：业务事务内写入 t_mq_send_record（与业务原子），
 *                    事务提交后真正发送，Confirm 更新状态，补偿任务兜底重发。
 * sendFast：高吞吐场景（秒杀）的轻量发送，Confirm 只打日志。
 */
@Service
public class ReliableMessagePublisher {
    private static final Logger log = LoggerFactory.getLogger(ReliableMessagePublisher.class);

    private final RabbitTemplate rabbit;
    private final ObjectMapper om;
    private final SendRecordDao sendRecordDao;

    public ReliableMessagePublisher(RabbitTemplate rabbit, ObjectMapper om, SendRecordDao sendRecordDao) {
        this.rabbit = rabbit;
        this.om = om;
        this.sendRecordDao = sendRecordDao;
    }

    /** 必须在 @Transactional 方法内调用，否则抛异常（强制事务纪律）。
     *  关键动作：事务内写本地消息表（与业务原子提交）+ 事务提交后真正发送。
     *  此后 Confirm 更新状态、补偿任务兜底重发 —— 三者闭环才是完整本地消息表。 */
    public void sendInTransaction(String exchange, String routingKey, String messageId, Object payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("sendInTransaction 必须在业务事务内调用");
        }
        String payloadJson = toJson(payload);
        sendRecordDao.insertPending(messageId, exchange, routingKey, payloadJson);   // 与业务同事务
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                doSend(exchange, routingKey, messageId, payloadJson);
            }
        });
    }

    /** 事务外快速发送（秒杀等场景：允许极小概率丢失，Confirm 失败仅告警） */
    public void sendFast(String exchange, String routingKey, String messageId, Object payload) {
        doSend(exchange, routingKey, messageId, toJson(payload));
    }

    /** 补偿重发：复用原始 payload 字符串与同一个 messageId（幂等的关键） */
    public void resend(String exchange, String routingKey, String messageId, String payloadJson) {
        doSend(exchange, routingKey, messageId, payloadJson);
    }

    private void doSend(String exchange, String routingKey, String messageId, String payloadJson) {
        try {
            byte[] body = payloadJson.getBytes(StandardCharsets.UTF_8);
            rabbit.send(exchange, routingKey, new Message(body, baseProps(messageId)),
                    new CorrelationData(messageId));
        } catch (Exception e) {
            // 本地消息表模式下：失败不影响业务（事务已提交），等补偿任务重发
            log.error("[发送失败-待补偿] messageId={} err={}", messageId, e.getMessage());
        }
    }

    private String toJson(Object payload) {
        try {
            return om.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("payload 序列化失败", e);
        }
    }

    private MessageProperties baseProps(String messageId) {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);   // 持久化三件套之消息（stage2/02）
        props.setMessageId(messageId);
        return props;
    }
}
