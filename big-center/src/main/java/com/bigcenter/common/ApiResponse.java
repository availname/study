package com.bigcenter.common;

/**
 * 统一响应基座（java-core/16-接口设计 规范落地：code 分段见 ErrorCode）
 * 0=成功；1xxx 用户 / 2xxx 短链 / 3xxx 红包 / 4xxx 支付 / 5xxx 系统
 */
public record ApiResponse<T>(int code, String msg, T data) {

    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(0, "ok", data); }

    public static ApiResponse<Void> ok() { return ok(null); }

    public static <T> ApiResponse<T> error(ErrorCode ec) { return new ApiResponse<>(ec.code(), ec.msg(), null); }

    public static <T> ApiResponse<T> error(int code, String msg) { return new ApiResponse<>(code, msg, null); }
}
