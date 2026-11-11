package com.bigcenter.auth.qrcode;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 扫码登录（java-core/12 题二落地）：
 * 二维码内含 uuid，状态机 WAITING → SCANNED → CONFIRMED 全程 Redis 承载，
 * TTL 5 分钟防过期码重放；PC 轮询状态接口，确认后签发 JWT（复用第 2 步 JwtService）。
 */
@Service
public class QrCodeService {

    static final String STATE_PREFIX = "bc:qrcode:state:";
    static final String USER_PREFIX = "bc:qrcode:user:";
    static final Duration QR_TTL = Duration.ofMinutes(5);      // 5 分钟防过期码重放

    /** 状态机常量 */
    static final String WAITING = "WAITING";        // 待扫
    static final String SCANNED = "SCANNED";        // 已扫待确认
    static final String CONFIRMED = "CONFIRMED";    // 已确认（可签发 token）

    private final StringRedisTemplate redis;
    private final com.bigcenter.auth.JwtService jwt;

    public QrCodeService(StringRedisTemplate redis, com.bigcenter.auth.JwtService jwt) {
        this.redis = redis;
        this.jwt = jwt;
    }

    /** ① PC 生成登录请求：uuid + TTL；二维码内容由前端渲染（uuid 即内容） */
    public Map<String, Object> create() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(STATE_PREFIX + uuid, WAITING, QR_TTL);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("uuid", uuid);
        view.put("expireSeconds", QR_TTL.toSeconds());
        return view;
    }

    /** ② 手机扫码（App 自带登录态）：WAITING → SCANNED；非 WAITING（过期/已确认）拒绝 */
    public boolean scan(String uuid, long appUserId) {
        String state = redis.opsForValue().get(STATE_PREFIX + uuid);
        if (!WAITING.equals(state)) {
            return false;               // 过期/非法/已确认——CAS 语义：只有 WAITING 能进 SCANNED
        }
        // SET with condition：原子推进状态（防两台手机并发扫——以 SETNX 类语义抢）
        Boolean won = redis.opsForValue().setIfPresent(STATE_PREFIX + uuid, SCANNED);
        if (!Boolean.TRUE.equals(won)) {
            return false;
        }
        redis.opsForValue().set(USER_PREFIX + uuid, String.valueOf(appUserId), QR_TTL);
        return true;
    }

    /** ③ 手机确认登录：SCANNED → CONFIRMED（同样条件推进） */
    public boolean confirm(String uuid, long appUserId) {
        String state = redis.opsForValue().get(STATE_PREFIX + uuid);
        if (!SCANNED.equals(state)) {
            return false;
        }
        // 归属校验：只有扫码那台设备能确认（防"扫码人≠确认人"的越权）
        String scanner = redis.opsForValue().get(USER_PREFIX + uuid);
        if (!String.valueOf(appUserId).equals(scanner)) {
            return false;
        }
        Boolean won = redis.opsForValue().setIfPresent(STATE_PREFIX + uuid, CONFIRMED);
        return Boolean.TRUE.equals(won);
    }

    /** ④ PC 轮询：返回当前状态；CONFIRMED 时顺带签发双 token 并清理（一次性） */
    public Map<String, Object> poll(String uuid) {
        String state = redis.opsForValue().get(STATE_PREFIX + uuid);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("state", state == null ? "EXPIRED" : state);   // key 过期 = EXPIRED
        if (CONFIRMED.equals(state)) {
            String uid = redis.opsForValue().get(USER_PREFIX + uuid);
            long userId = Long.parseLong(uid);
            view.put("accessToken", jwt.signAccess(userId));
            view.put("refreshToken", jwt.issueRefresh(userId));
            // 一次性：token 发走即清理（防轮询重复取 token）
            redis.delete(java.util.List.of(STATE_PREFIX + uuid, USER_PREFIX + uuid));
        }
        return view;
    }
}
