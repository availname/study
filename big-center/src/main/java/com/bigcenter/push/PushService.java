package com.bigcenter.push;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bigcenter.push.mapper.PushMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息推送服务（java-core/14 题三落地）：
 * 通道三层——在线 SSE 实时推 → 离线落库 → 拉取兜底；
 * 在线路由表：Redis hash uid→"online"（集群时为网关实例标识）；
 * 未读数 = 最新消息 ID − 已读位点（读扩散差值 O(1)，亿级用户也只是一条公告）。
 */
@Service
public class PushService {
    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    static final String ROUTE_PREFIX = "bc:push:route:";
    static final String READ_POINT_PREFIX = "bc:push:readpoint:";

    /** 本实例在线用户的 SSE 连接（集群部署时经 MQ fanout 广播到各实例，各自下发） */
    private final Map<Long, SseEmitter> onlineEmitters = new ConcurrentHashMap<>();

    private final PushMessageMapper messageMapper;
    private final StringRedisTemplate redis;

    public PushService(PushMessageMapper messageMapper, StringRedisTemplate redis) {
        this.messageMapper = messageMapper;
        this.redis = redis;
    }

    /** SSE 连接建立：注册路由 + 本实例持有 emitter */
    public SseEmitter connect(long userId) {
        SseEmitter emitter = new SseEmitter(0L);        // 不超时（心跳由前端 ping 维持）
        onlineEmitters.put(userId, emitter);
        redis.opsForValue().set(ROUTE_PREFIX + userId, "online", Duration.ofHours(24));
        emitter.onCompletion(() -> offline(userId));
        emitter.onTimeout(() -> offline(userId));
        return emitter;
    }

    private void offline(long userId) {
        onlineEmitters.remove(userId);
        redis.delete(ROUTE_PREFIX + userId);            // 心跳断开删路由 → 自动转离线通道（14 篇追问①）
    }

    /** 发送私信：在线 SSE 直推，离线落库（两条通道共用——不丢不重，消息 ID 去重） */
    public PushMessage sendPrivate(long fromUserId, long toUserId, String title, String content) {
        PushMessage msg = new PushMessage();
        msg.setFromUserId(fromUserId);
        msg.setToUserId(toUserId);
        msg.setTitle(title);
        msg.setContent(content);
        messageMapper.insert(msg);                      // 先落库拿 ID（顺序性：按 msg_id 单调）
        tryPushOnline(toUserId, msg);
        return msg;
    }

    private void tryPushOnline(long userId, PushMessage msg) {
        SseEmitter emitter = onlineEmitters.get(userId);
        if (emitter == null) {
            log.info("用户离线，消息已落库待拉取 userId={} msgId={}", userId, msg.getId());
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("message").data(
                    Map.of("msgId", msg.getId(), "title", msg.getTitle(), "content", msg.getContent())));
        } catch (IOException e) {
            onlineEmitters.remove(userId);              // 推送失败连接已坏 → 转离线通道
            log.warn("SSE 推送失败转离线 userId={}", userId);
        }
    }

    /** 拉取兜底：重连后补齐位点之后的消息（按 ID 去重保证不丢不重） */
    public List<PushMessage> pullAfter(long userId) {
        long readPoint = readPoint(userId);
        return messageMapper.selectList(new LambdaQueryWrapper<PushMessage>()
                .eq(PushMessage::getToUserId, userId)
                .gt(PushMessage::getId, readPoint)
                .orderByAsc(PushMessage::getId));
    }

    /** 已读位点推进 */
    public void markRead(long userId, long msgId) {
        redis.opsForValue().set(READ_POINT_PREFIX + userId, String.valueOf(msgId));
    }

    public long readPoint(long userId) {
        String p = redis.opsForValue().get(READ_POINT_PREFIX + userId);
        return p == null ? 0L : Long.parseLong(p);
    }

    /** 未读数 = 最新消息 ID − 已读位点（O(1) 差值，私信场景用明细表 max(id)） */
    public long unreadCount(long userId) {
        Long maxId = messageMapper.selectList(new LambdaQueryWrapper<PushMessage>()
                        .eq(PushMessage::getToUserId, userId)
                        .orderByDesc(PushMessage::getId).last("LIMIT 1"))
                .stream().findFirst().map(PushMessage::getId).orElse(0L);
        return Math.max(0, maxId - readPoint(userId));
    }

    /** 本实例在线连接数（监控用） */
    public int onlineCount() {
        return onlineEmitters.size();
    }
}
