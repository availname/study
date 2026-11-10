package com.example.mqmall.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 媒体处理流水线拓扑（stage5 视频平台场景）。
 * 管道：media.submitted → 转码 → media.transcoded → 审核 → media.audited → 上架
 * 广播：上架完成 → media.published.fanout → 推荐索引 / 粉丝通知（新增下游零改动）
 */
@Configuration
public class MediaTopologyConfig {

    /** 媒体事件交换机（Topic）：管道内各环节的串联都走它 */
    @Bean
    public TopicExchange mediaEventsExchange() {
        return new TopicExchange("media.events", true, false);
    }

    /** 发布完成广播（Fanout）：谁关心"新视频上线"谁就绑一个队列，生产者零改动（stage1/04） */
    @Bean
    public FanoutExchange mediaPublishedFanout() {
        return new FanoutExchange("media.published.fanout", true, false);
    }

    private Queue mediaQueue(String name) {
        // 与商城业务队列共用通用死信交换机 dlx.exchange（stage3/05）
        return QueueBuilder.durable(name)
                .deadLetterExchange("dlx.exchange")
                .deadLetterRoutingKey("mall.dead")
                .build();
    }

    @Bean public Queue mediaSubmittedQueue()  { return mediaQueue("media.submitted.queue"); }
    @Bean public Queue mediaTranscodedQueue() { return mediaQueue("media.transcoded.queue"); }
    @Bean public Queue mediaAuditedQueue()    { return mediaQueue("media.audited.queue"); }
    @Bean public Queue mediaRecommendQueue()  { return mediaQueue("media.recommend.queue"); }
    @Bean public Queue mediaNotifyQueue()     { return mediaQueue("media.notify.queue"); }

    @Bean public Binding submittedBinding()  { return BindingBuilder.bind(mediaSubmittedQueue()).to(mediaEventsExchange()).with("media.submitted"); }
    @Bean public Binding transcodedBinding() { return BindingBuilder.bind(mediaTranscodedQueue()).to(mediaEventsExchange()).with("media.transcoded"); }
    @Bean public Binding auditedBinding()    { return BindingBuilder.bind(mediaAuditedQueue()).to(mediaEventsExchange()).with("media.audited"); }
    @Bean public Binding recommendBinding()  { return BindingBuilder.bind(mediaRecommendQueue()).to(mediaPublishedFanout()); }
    @Bean public Binding notifyBinding()     { return BindingBuilder.bind(mediaNotifyQueue()).to(mediaPublishedFanout()); }
}
