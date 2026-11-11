package com.bigcenter.push;

import com.bigcenter.auth.UserContext;
import com.bigcenter.common.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/** 推送接口（/api/push/**，JWT 保护）——SSE 端点 /api/push/connect 前端 EventSource 订阅 */
@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushService pushService;

    public PushController(PushService pushService) {
        this.pushService = pushService;
    }

    /** SSE 连接（在线通道建立） */
    @GetMapping("/connect")
    public SseEmitter connect() {
        return pushService.connect(UserContext.get());
    }

    /** 发私信（from=登录态，不允许冒名——信任的只有登录态身份） */
    @PostMapping("/send")
    public ApiResponse<Long> send(@RequestParam long toUserId, @RequestParam String title, @RequestParam String content) {
        PushMessage msg = pushService.sendPrivate(UserContext.get(), toUserId, title, content);
        return ApiResponse.ok(msg.getId());
    }

    /** 拉取兜底：位点之后的消息 */
    @GetMapping("/pull")
    public ApiResponse<List<PushMessage>> pull() {
        return ApiResponse.ok(pushService.pullAfter(UserContext.get()));
    }

    /** 已读位点推进 */
    @PostMapping("/read")
    public ApiResponse<Void> markRead(@RequestParam long msgId) {
        pushService.markRead(UserContext.get(), msgId);
        return ApiResponse.ok();
    }

    /** 未读数（位点差值 O(1)） */
    @GetMapping("/unread")
    public ApiResponse<Map<String, Object>> unread() {
        return ApiResponse.ok(Map.of("unread", pushService.unreadCount(UserContext.get())));
    }
}
