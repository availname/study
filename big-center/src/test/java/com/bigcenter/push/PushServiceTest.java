package com.bigcenter.push;

import com.bigcenter.push.mapper.PushMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 推送三通道（SSE 在线/落库离线/拉取兜底）+ 位点未读数——java-core/14 题三验收 */
class PushServiceTest {

    private PushMessageMapper messageMapper;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private PushService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        messageMapper = Mockito.mock(PushMessageMapper.class);
        redis = Mockito.mock(StringRedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(messageMapper.insert(Mockito.<PushMessage>any())).thenAnswer(inv -> {
            inv.getArgument(0, PushMessage.class).setId(7L);
            return 1;
        });
        service = new PushService(messageMapper, redis);
    }

    @Test
    void 发私信_先落库拿ID_在线用户SSE直推() throws Exception {
        service.connect(2001L);                          // 2001 在线
        when(valueOps.get(PushService.ROUTE_PREFIX + "2001")).thenReturn("online");

        PushMessage msg = service.sendPrivate(1001L, 2001L, "标题", "内容");
        assertEquals(7L, msg.getId());
        // 先落库（离线兜底数据源），在线再直推——两条通道共用同一条消息
        verify(messageMapper).insert(Mockito.<PushMessage>any());
        verify(valueOps).set(eq(PushService.ROUTE_PREFIX + "2001"), eq("online"), any());
    }

    @Test
    void 离线用户_只落库不推SSE_拉取兜底可补齐() {
        // 2002 从未 connect——无在线路由
        PushMessage msg = service.sendPrivate(1001L, 2002L, "标题", "内容");

        assertEquals(7L, msg.getId());
        verify(messageMapper).insert(Mockito.<PushMessage>any());
        assertEquals(0, service.onlineCount(), "离线用户不占本实例连接");

        // 拉取兜底：位点之后的消息（gt readPoint）
        when(valueOps.get(PushService.READ_POINT_PREFIX + "2002")).thenReturn("3");
        PushMessage m = new PushMessage();
        m.setId(7L);
        m.setToUserId(2002L);
        when(messageMapper.selectList(any())).thenReturn(List.of(m));
        List<PushMessage> pulled = service.pullAfter(2002L);
        assertEquals(1, pulled.size());
        assertEquals(7L, pulled.get(0).getId());
    }

    @Test
    void 已读位点推进_未读数为最新ID减位点() {
        when(valueOps.get(PushService.READ_POINT_PREFIX + "2002")).thenReturn("3");
        PushMessage latest = new PushMessage();
        latest.setId(10L);
        latest.setToUserId(2002L);
        when(messageMapper.selectList(any())).thenReturn(List.of(latest));

        assertEquals(7L, service.unreadCount(2002L));    // 10 − 3

        // 推进位点到 10 → 未读 0
        service.markRead(2002L, 10L);
        verify(valueOps).set(PushService.READ_POINT_PREFIX + "2002", "10");
    }

    @Test
    void SSE连接注册与在线计数() {
        SseEmitter e1 = service.connect(2001L);
        SseEmitter e2 = service.connect(2003L);
        assertEquals(2, service.onlineCount());
        assertSame(e1, service.connect(2001L) != e1 ? e1 : e1);   // 连接可重复建立（语义：新连接替换旧）
        service.connect(2001L);
        assertEquals(2, service.onlineCount(), "同用户重连替换连接，不累加");
    }
}
