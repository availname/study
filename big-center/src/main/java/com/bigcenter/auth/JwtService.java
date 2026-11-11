package com.bigcenter.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/**
 * JWT 双 token 服务（java-core/19 第三节落地）：
 * <ul>
 *   <li>access token：15 分钟，HS256 验签零查询；登出后进黑名单（撤销窗口只 15 分钟）</li>
 *   <li>refresh token：随机串存 Redis 7 天（可撤销——旋转/登出 DEL 即刻生效）</li>
 * </ul>
 * 核心认知（面试一句话）：JWT 的"无状态"与"可撤销"是根本矛盾——黑名单是把状态加回来。
 */
@Service
public class JwtService {

    static final String BLACKLIST_PREFIX = "bc:jwt:blacklist:";
    static final String REFRESH_PREFIX = "bc:jwt:refresh:";
    static final long ACCESS_TTL_MS = 15 * 60 * 1000L;     // 15 分钟
    static final Duration REFRESH_TTL = Duration.ofDays(7); // 7 天

    private final SecretKey key;
    private final StringRedisTemplate redis;

    public JwtService(@Value("${big-center.jwt.secret}") String secret, StringRedisTemplate redis) {
        // 教学项目：由配置注入固定密钥（多实例共享同一密钥才能互验）；生产建议 RS256 非对称 + 环境变量托管
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT 密钥至少 32 字节（HS256 要求 256 位）");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.redis = redis;
    }

    /** 签发 access token（payload 只放 userId 与过期时间——明文纪律：不放敏感信息） */
    public String signAccess(long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TTL_MS))
                .signWith(key)
                .compact();
    }

    /**
     * 验签 + 黑名单检查。返回 userId；无效/过期/已登出返回 null（由拦截器统一 401）。
     * 代价说明（19 篇）：黑名单让每请求多一次 Redis 查——JWT 退化成"方案② + 签名"。
     */
    public Long verifyAccess(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            if (Boolean.TRUE.equals(redis.hasKey(BLACKLIST_PREFIX + token))) {
                return null;                    // 已登出（黑名单 TTL=剩余有效期，过期自动出列）
            }
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return null;                        // 伪造/过期/格式错——不区分细节，统一拒（不泄露探测信息）
        }
    }

    /** 登出：access token 进黑名单（TTL=剩余有效期，到期自动清除） */
    public void logout(String accessToken, long userId) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(accessToken).getPayload();
            long remainMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainMs > 0) {
                redis.opsForValue().set(BLACKLIST_PREFIX + accessToken, String.valueOf(userId),
                        Duration.ofMillis(remainMs));
            }
        } catch (JwtException ignored) {
            // 已过期/伪造的 token 无需拉黑
        }
    }

    /** 签发 refresh token：随机串存 Redis（key 带 userId 便于"改密全下线"按用户清除） */
    public String issueRefresh(long userId) {
        String refresh = java.util.UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(REFRESH_PREFIX + refresh, String.valueOf(userId), REFRESH_TTL);
        return refresh;
    }

    /** 刷新：校验 Redis 存在 → 签发新 access；返回 null 表示 refresh 无效/已撤销 */
    public Long verifyRefresh(String refreshToken) {
        String uid = redis.opsForValue().get(REFRESH_PREFIX + refreshToken);
        return uid == null ? null : Long.parseLong(uid);
    }

    /** 登出/旋转：撤销 refresh token */
    public void revokeRefresh(String refreshToken) {
        redis.delete(REFRESH_PREFIX + refreshToken);
    }
}
