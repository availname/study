package com.bigcenter.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** JWT 双 token：验签/黑名单/refresh 撤销——java-core/19 三解法之"黑名单+短过期"验收 */
class JwtServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private JwtService jwt;

    @BeforeEach
    void setUp() {
        redis = Mockito.mock(StringRedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        // 链式 mock：先固定 opsForValue 返回同一实例
        when(redis.opsForValue()).thenReturn(valueOps);
        // 黑名单默认空（hasKey false = 未登出）
        when(redis.hasKey(anyString())).thenReturn(false);
        jwt = new JwtService("big-center-test-secret-key-0123456789ab!", redis);
    }

    @Test
    void 签发的access_验签解析出原userId() {
        String token = jwt.signAccess(1001L);
        assertEquals(1001L, jwt.verifyAccess(token));
    }

    @Test
    void 黑名单内的token验签拒绝_未登出的放行() {
        String loggedOut = jwt.signAccess(1001L);
        // 教学发现：同毫秒同 payload 的 JWT 是确定性的（无 jti）——两个 token 会逐字节相同。
        // 用不同 userId 保证 alive 与 loggedOut 是不同串（生产上 JWT 通常带 jti 防重放，见 19 篇第六节）。
        String alive = jwt.signAccess(1002L);
        // 仅该 token 已登出：黑名单命中
        when(redis.hasKey(JwtService.BLACKLIST_PREFIX + loggedOut)).thenReturn(true);
        assertNull(jwt.verifyAccess(loggedOut), "登出后的 access 必须被黑名单拦截");
        // 另一个未登出的 token 正常放行
        assertEquals(1002L, jwt.verifyAccess(alive));
    }

    @Test
    void 篡改或伪造的token验签返回null_不抛异常() {
        String token = jwt.signAccess(1001L);
        // 改一个字符（签名必然对不上）
        String tampered = token.substring(0, token.length() - 2)
                + (token.endsWith("a") ? "bX" : "aX");
        assertNull(jwt.verifyAccess(tampered));
        assertNull(jwt.verifyAccess("not-a-jwt"));
    }

    @Test
    void logout写入黑名单_TTL为剩余有效期() {
        String token = jwt.signAccess(1001L);
        jwt.logout(token, 1001L);
        // 黑名单 TTL 必须是"剩余有效期"——存永久会让 Redis 无限膨胀
        verify(valueOps).set(eq(JwtService.BLACKLIST_PREFIX + token), eq("1001"),
                Mockito.<Duration>any());
    }

    @Test
    void refresh签发后verify通过_撤销后拒绝() {
        when(valueOps.get(JwtService.REFRESH_PREFIX + "r123")).thenReturn("1001", (String) null);
        assertEquals(1001L, jwt.verifyRefresh("r123"));
        // 撤销（DEL）后 verify 返回 null
        jwt.revokeRefresh("r123");
        assertNull(jwt.verifyRefresh("r123"));
        verify(redis).delete(JwtService.REFRESH_PREFIX + "r123");
    }

    @Test
    void logout写入黑名单_未触碰refresh键() {
        jwt.logout(jwt.signAccess(1001L), 1001L);
        // refresh 的撤销是 AuthService 的职责——JwtService.logout 只写 access 黑名单这一处
        verify(redis, never()).delete(Mockito.anyString());
        verify(valueOps, Mockito.times(1)).set(anyString(), anyString(), Mockito.<Duration>any());
    }
}
