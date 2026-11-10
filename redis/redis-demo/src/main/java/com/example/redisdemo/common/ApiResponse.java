package com.example.redisdemo.common;

/** 统一响应体 */
public record ApiResponse<T>(int code, String msg, T data) {
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(0, "ok", data); }
    public static <T> ApiResponse<T> error(String msg) { return new ApiResponse<>(400, msg, null); }
}
