package com.example.learn.common;

public record ApiResponse<T>(int code, String msg, T data) {
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(0, "ok", data); }
    public static <T> ApiResponse<T> error(int code, String msg) { return new ApiResponse<>(code, msg, null); }
    public static <T> ApiResponse<T> error(String msg) { return new ApiResponse<>(400, msg, null); }
}
