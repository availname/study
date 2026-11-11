package com.bigcenter.common;

/**
 * 错误码分段（java-core/16-接口设计：错误码即文档，见 README 对号入座表）
 * 0 成功；1xxx 用户 / 2xxx 短链 / 3xxx 红包 / 4xxx 支付 / 5xxx 系统
 */
public enum ErrorCode {
    // ---- 1xxx 用户 ----
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户名已存在"),
    USER_PASSWORD_WRONG(1003, "用户名或密码错误"),
    TOKEN_INVALID(1004, "令牌无效或已过期"),
    TOKEN_LOGGED_OUT(1005, "令牌已登出"),
    PERMISSION_DENIED(1006, "无权访问该资源"),

    // ---- 2xxx 短链 ----
    SHORT_LINK_NOT_FOUND(2001, "短链不存在"),
    SHORT_LINK_EXPIRED(2002, "短链已过期"),

    // ---- 3xxx 红包 ----
    REDPACK_NOT_FOUND(3001, "红包不存在"),
    REDPACK_GRABBED_OUT(3002, "红包已抢完"),
    REDPACK_ALREADY_GRABBED(3003, "已抢过该红包"),
    REDPACK_EXPIRED(3004, "红包已过期退款中"),

    // ---- 4xxx 支付 ----
    PAY_ORDER_NOT_FOUND(4001, "支付单不存在"),
    PAY_STATUS_CONFLICT(4002, "支付单状态冲突（状态机 CAS 失败）"),
    PAY_CHANNEL_UNSUPPORTED(4003, "不支持的支付渠道"),
    PAY_SIGN_INVALID(4004, "回调验签失败"),

    // ---- 5xxx 系统 ----
    RATE_LIMITED(5001, "请求过于频繁，请稍后再试"),
    SYSTEM_ERROR(5000, "系统繁忙");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) { this.code = code; this.msg = msg; }

    public int code() { return code; }
    public String msg() { return msg; }
}
