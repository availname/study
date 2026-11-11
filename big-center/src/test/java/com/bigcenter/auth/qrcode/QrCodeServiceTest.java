package com.bigcenter.auth.qrcode;

import com.bigcenter.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 扫码登录状态机：WAITING→SCANNED→CONFIRMED 条件推进/归属校验/token 一次性——12 题二验收 */
class QrCodeServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private JwtService jwt;
    private QrCodeService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = Mockito.mock(StringRedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        jwt = Mockito.mock(JwtService.class);
        service = new QrCodeService(redis, jwt);
    }

    @Test
    void 创建二维码_WAITING状态_5分钟TTL() {
        Map<String, Object> view = service.create();
        String uuid = (String) view.get("uuid");
        verify(valueOps).set(eq(QrCodeService.STATE_PREFIX + uuid), eq(QrCodeService.WAITING), any());
        assertEquals(300L, view.get("expireSeconds"));      // 5 分钟防过期码重放
    }

    @Test
    void 状态机正常推进_WAITING经SCANNED到CONFIRMED() {
        // create
        when(valueOps.get(QrCodeService.STATE_PREFIX + "u1")).thenReturn(QrCodeService.WAITING);
        when(valueOps.setIfPresent(QrCodeService.STATE_PREFIX + "u1", QrCodeService.SCANNED)).thenReturn(true);
        assertTrue(service.scan("u1", 2001L));
        verify(valueOps).set(QrCodeService.USER_PREFIX + "u1", "2001", QrCodeService.QR_TTL);

        // confirm：只有 WAITING→SCANNED→CONFIRMED 单向推进
        when(valueOps.get(QrCodeService.STATE_PREFIX + "u1")).thenReturn(QrCodeService.SCANNED);
        when(valueOps.get(QrCodeService.USER_PREFIX + "u1")).thenReturn("2001");
        when(valueOps.setIfPresent(QrCodeService.STATE_PREFIX + "u1", QrCodeService.CONFIRMED)).thenReturn(true);
        assertTrue(service.confirm("u1", 2001L));
    }

    @Test
    void 已确认或过期的码不能再扫_条件推进拒绝() {
        when(valueOps.get(QrCodeService.STATE_PREFIX + "u2")).thenReturn(QrCodeService.CONFIRMED);
        assertFalse(service.scan("u2", 2001L), "已确认的码再扫必须拒绝");
        when(valueOps.get(QrCodeService.STATE_PREFIX + "u3")).thenReturn(null);
        assertFalse(service.scan("u3", 2001L), "过期（key 消失）的码必须拒绝");
        verify(valueOps, Mockito.never()).set(eq(QrCodeService.USER_PREFIX + "u3"), anyString(), any());
    }

    @Test
    void 确认人必须等于扫码人_防越权确认() {
        when(valueOps.get(QrCodeService.STATE_PREFIX + "u4")).thenReturn(QrCodeService.SCANNED);
        when(valueOps.get(QrCodeService.USER_PREFIX + "u4")).thenReturn("2001");
        // 另一个设备（uid 2999）拿着同一 uuid 来确认——必须拒绝
        assertFalse(service.confirm("u4", 2999L), "确认人≠扫码人应拒绝（越权防护）");
    }

    @Test
    void 轮询确认后签发双token并清理_第二次轮询拿不到() {
        when(valueOps.get(QrCodeService.STATE_PREFIX + "u5"))
                .thenReturn(QrCodeService.CONFIRMED, (String) null);   // 第一次 CONFIRMED，清理后 null
        when(valueOps.get(QrCodeService.USER_PREFIX + "u5")).thenReturn("2001");
        when(jwt.signAccess(2001L)).thenReturn("access-1");
        when(jwt.issueRefresh(2001L)).thenReturn("refresh-1");

        Map<String, Object> first = service.poll("u5");
        assertEquals(QrCodeService.CONFIRMED, first.get("state"));
        assertEquals("access-1", first.get("accessToken"));
        assertEquals("refresh-1", first.get("refreshToken"));
        // 一次性：token 发走即清理两个 key（防轮询重复取）
        verify(redis).delete(java.util.List.of(QrCodeService.STATE_PREFIX + "u5", QrCodeService.USER_PREFIX + "u5"));

        // 第二次轮询：state 已删 → EXPIRED（视图层兜底），无 token
        Map<String, Object> second = service.poll("u5");
        assertEquals("EXPIRED", second.get("state"));
        assertFalse(second.containsKey("accessToken"));
    }

    @Test
    void 轮询WAITING状态_只返回状态不签发() {
        when(valueOps.get(QrCodeService.STATE_PREFIX + "u6")).thenReturn(QrCodeService.WAITING);
        Map<String, Object> view = service.poll("u6");
        assertEquals(QrCodeService.WAITING, view.get("state"));
        assertFalse(view.containsKey("accessToken"));
        verify(jwt, Mockito.never()).signAccess(Mockito.anyLong());
    }
}
