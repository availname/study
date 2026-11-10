package com.example.mqmall.media;

import com.example.mqmall.common.ApiResponse;
import com.example.mqmall.common.BizException;
import com.example.mqmall.common.Ids;
import com.example.mqmall.messaging.MessageDedupService;
import com.example.mqmall.messaging.Messages.VideoEvent;
import com.example.mqmall.messaging.ReliableMessagePublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 视频接口（stage5 真实场景教学：上传完成的秒回 + 管道状态轮询 + 人工审核卡点）。
 */
@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final MediaTaskDao taskDao;
    private final MediaPipelineConsumers pipeline;
    private final ReliableMessagePublisher publisher;
    private final MessageDedupService dedup;

    public VideoController(MediaTaskDao taskDao, MediaPipelineConsumers pipeline,
                           ReliableMessagePublisher publisher, MessageDedupService dedup) {
        this.taskDao = taskDao;
        this.pipeline = pipeline;
        this.publisher = publisher;
        this.dedup = dedup;
    }

    /** 提交视频（模拟"文件上传完成"）：秒回 videoId，转码/审核/上架全部异步进行。
     *  建任务 + 发消息必须同事务（sendInTransaction 强制事务纪律；生产中此逻辑应下沉到 Service 层） */
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<String> submit(@RequestParam String title) {
        String videoId = "V" + Ids.nextId();
        taskDao.insert(videoId, title);
        publisher.sendInTransaction("media.events", "media.submitted",
                "media-submitted-" + videoId, new VideoEvent(videoId, title));
        return ApiResponse.ok(videoId);
    }

    /** 查询处理状态（前端轮询："转码中 30%..." 的数据来源） */
    @GetMapping("/{videoId}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String videoId) {
        return taskDao.find(videoId)
                .<ApiResponse<Map<String, Object>>>map(t -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("videoId", t.videoId());
                    view.put("title", t.title());
                    view.put("status", t.status());
                    view.put("statusDesc", statusDesc(t.status()));
                    view.put("failReason", t.failReason());
                    return ApiResponse.ok(view);
                })
                .orElseGet(() -> ApiResponse.error(404, "视频不存在"));
    }

    /** 失败任务人工重提：FAILED → UPLOADED → 重新入管道（配 demo：标题含 FAIL 的视频）。
     *  ⚠ 必须先清幂等键再重发：失败链路中"状态机 CAS 短路 return"会绕过 catch 里的
     *  dedup.fail 清理，残留的键会把重发消息当成"重复"拦下（真实踩坑，见踩坑实录坑九）。 */
    @PostMapping("/{videoId}/retry")
    public ApiResponse<Void> retry(@PathVariable String videoId) {
        if (taskDao.resetForRetry(videoId) == 0) {
            throw new BizException("只有 FAILED 状态的任务可以重试");
        }
        MediaTaskDao.TaskRow task = taskDao.find(videoId).orElseThrow();
        dedup.fail("media-submitted-" + videoId);   // 清残留幂等键，让重发消息能被处理
        publisher.sendFast("media.events", "media.submitted",
                "media-submitted-" + videoId, new VideoEvent(videoId, task.title()));
        return ApiResponse.ok(null);
    }

    /** 人工审核卡点（标题含 HUMAN 的视频会停在 HUMAN_AUDIT 状态等你） */
    @PostMapping("/{videoId}/audit")
    public ApiResponse<Void> audit(@PathVariable String videoId,
                                   @RequestParam(defaultValue = "true") boolean pass) {
        MediaTaskDao.TaskRow task = taskDao.find(videoId)
                .orElseThrow(() -> new BizException("视频不存在"));
        if (!MediaTaskDao.HUMAN_AUDIT.equals(task.status())) {
            throw new BizException("只有 HUMAN_AUDIT 状态可以人工审核，当前: " + task.status());
        }
        pipeline.finishHumanAudit(new VideoEvent(videoId, task.title()), pass);
        return ApiResponse.ok(null);
    }

    /** 各状态任务数（管理界面"处理中/待人工/已发布"大盘的数据来源） */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Long>> stats() {
        return ApiResponse.ok(taskDao.countByStatus());
    }

    private static String statusDesc(String s) {
        return switch (s) {
            case "UPLOADED" -> "已上传，排队等待转码";
            case "TRANSCODING" -> "转码中（FFmpeg 处理，分钟级）";
            case "TRANSCODED" -> "转码完成，排队等待审核";
            case "AUDITING" -> "AI 审核中";
            case "HUMAN_AUDIT" -> "等待人工审核";
            case "AUDITED" -> "审核通过，排队上架";
            case "PUBLISHED" -> "已发布（推荐索引/粉丝通知进行中）";
            case "REJECTED" -> "审核不通过";
            case "FAILED" -> "处理失败（可调用 /retry 重提）";
            default -> "未知状态";
        };
    }
}
