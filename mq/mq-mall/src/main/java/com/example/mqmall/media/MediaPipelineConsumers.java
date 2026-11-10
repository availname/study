package com.example.mqmall.media;

import com.example.mqmall.messaging.MessageDedupService;
import com.example.mqmall.messaging.Messages.VideoEvent;
import com.example.mqmall.messaging.ReliableMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 视频处理流水线的全部消费者（stage5 真实场景教学）。
 *
 * 管道（队列串联，教程 stage5/02）：
 *   media.submitted → ①转码 → media.transcoded → ②审核 → media.audited → ③上架
 * 广播（fanout，教程 stage5/03）：
 *   上架完成 → media.published.fanout → ④推荐索引 ∥ ⑤粉丝通知
 *
 * 每一级的错误语义示范（教程 stage5/04）：
 *   - 确定性任务失败（如标题含 FAIL）→ 标记 FAILED + 抛异常走重试/死信 + 人工 retry 端点重提
 *   - 审核不通过（BLOCK）/ 需人工（HUMAN）→ 正常消费、状态机分叉，不抛异常
 *   - 状态机 CAS（MediaTaskDao.casStatus）天然挡住重复/乱序消息
 */
@Component
public class MediaPipelineConsumers {
    private static final Logger log = LoggerFactory.getLogger(MediaPipelineConsumers.class);

    private final MediaTaskDao taskDao;
    private final MessageDedupService dedup;
    private final ReliableMessagePublisher publisher;

    public MediaPipelineConsumers(MediaTaskDao taskDao, MessageDedupService dedup,
                                  ReliableMessagePublisher publisher) {
        this.taskDao = taskDao;
        this.dedup = dedup;
        this.publisher = publisher;
    }

    // ============ ① 转码：最重的一环，sleep 模拟 1.5s FFmpeg ============

    @RabbitListener(queues = "media.submitted.queue")
    public void onSubmitted(VideoEvent event) {
        String messageId = "media-submitted-" + event.videoId();
        if (!dedup.tryConsume("media-transcode", messageId)) {
            log.info("[转码] 重复消息跳过 {}", event.videoId());
            return;
        }
        try {
            if (taskDao.casStatus(event.videoId(), MediaTaskDao.UPLOADED, MediaTaskDao.TRANSCODING) == 0) {
                log.info("[转码] 状态不是 UPLOADED，跳过（重复/乱序消息） {}", event.videoId());
                return;
            }
            Thread.sleep(1500);   // 模拟转码耗时（真实：FFmpeg 多码率转码，分钟级）
            // 确定性失败演示：标题含 FAIL 的视频转码必失败 → 重试3次 → 死信 → 人工 retry 重提
            if (event.title().contains("FAIL")) {
                throw new RuntimeException("模拟转码失败（标题含 FAIL）");
            }
            taskDao.casStatus(event.videoId(), MediaTaskDao.TRANSCODING, MediaTaskDao.TRANSCODED);
            publisher.sendFast("media.events", "media.transcoded",
                    "media-transcoded-" + event.videoId(), event);
            log.info("[转码] 完成 {}", event.videoId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (Exception e) {
            taskDao.markFailed(event.videoId(), e.getMessage());
            dedup.fail(messageId);   // 回滚去重键：人工 retry 重提时才能重新处理
            throw e;                 // 本地重试 3 次 → 耗尽进死信落库
        }
    }

    // ============ ② 审核：AI 秒审 + 人工卡点 + 分叉 ============

    @RabbitListener(queues = "media.transcoded.queue")
    public void onTranscoded(VideoEvent event) {
        String messageId = "media-transcoded-" + event.videoId();
        if (!dedup.tryConsume("media-audit", messageId)) {
            log.info("[审核] 重复消息跳过 {}", event.videoId());
            return;
        }
        if (taskDao.casStatus(event.videoId(), MediaTaskDao.TRANSCODED, MediaTaskDao.AUDITING) == 0) {
            log.info("[审核] 状态不是 TRANSCODED，跳过 {}", event.videoId());
            return;
        }

        if (event.title().contains("BLOCK")) {
            taskDao.casStatus(event.videoId(), MediaTaskDao.AUDITING, MediaTaskDao.REJECTED);
            log.info("[审核] AI 审核不通过，已拒绝下架 {}", event.videoId());
            return;
        }
        if (event.title().contains("HUMAN")) {
            taskDao.casStatus(event.videoId(), MediaTaskDao.AUDITING, MediaTaskDao.HUMAN_AUDIT);
            log.info("[审核] AI 不确定 → 进入人工审核队列（调 POST /api/videos/{}/audit 处理）", event.videoId());
            return;
        }
        taskDao.casStatus(event.videoId(), MediaTaskDao.AUDITING, MediaTaskDao.AUDITED);
        publisher.sendFast("media.events", "media.audited",
                "media-audited-" + event.videoId(), event);
        log.info("[审核] AI 审核通过 {}", event.videoId());
    }

    /** 人工审核结果推进（由 VideoController.audit 调用；通过后接管道第三级） */
    public void finishHumanAudit(VideoEvent event, boolean pass) {
        if (pass) {
            taskDao.casStatus(event.videoId(), MediaTaskDao.HUMAN_AUDIT, MediaTaskDao.AUDITED);
            publisher.sendFast("media.events", "media.audited",
                    "media-audited-" + event.videoId(), event);
            log.info("[审核] 人工通过 {}", event.videoId());
        } else {
            taskDao.casStatus(event.videoId(), MediaTaskDao.HUMAN_AUDIT, MediaTaskDao.REJECTED);
            log.info("[审核] 人工拒绝 {}", event.videoId());
        }
    }

    // ============ ③ 上架：CDN 预热、元数据落检索库（模拟） ============

    @RabbitListener(queues = "media.audited.queue")
    public void onAudited(VideoEvent event) throws InterruptedException {
        String messageId = "media-audited-" + event.videoId();
        if (!dedup.tryConsume("media-publish", messageId)) {
            log.info("[上架] 重复消息跳过 {}", event.videoId());
            return;
        }
        if (taskDao.casStatus(event.videoId(), MediaTaskDao.AUDITED, MediaTaskDao.PUBLISHED) == 0) {
            log.info("[上架] 状态不是 AUDITED，跳过 {}", event.videoId());
            return;
        }
        Thread.sleep(500);   // 模拟 CDN 预热
        // 发布完成 → fanout 广播：所有下游各自消费，互不影响（新增下游零改动）
        publisher.sendFast("media.published.fanout", "",
                "media-published-" + event.videoId(), event);
        log.info("[上架] 已发布并广播 {}", event.videoId());
    }

    // ============ ④⑤ 发布完成后的广播下游（各自独立消费） ============

    @RabbitListener(queues = "media.recommend.queue")
    public void onRecommend(VideoEvent event) {
        log.info("[推荐索引] 新视频入库可检索: {} {}", event.videoId(), event.title());
    }

    @RabbitListener(queues = "media.notify.queue")
    public void onNotify(VideoEvent event) {
        // 生产：分批拉粉丝列表、限速推送（百万粉丝 = 洪峰缓冲，见教程 stage5/03）
        log.info("[粉丝通知] UP主《{}》发布已推送给粉丝（模拟分批推送）", event.title());
    }
}
