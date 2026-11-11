package com.bigcenter.auth;

/** 用户上下文（ThreadLocal——拦截器写入，业务层读取，afterCompletion 清理；与 learn-center 同款三件套） */
public final class UserContext {
    private static final ThreadLocal<Long> CTX = new ThreadLocal<>();

    private UserContext() {}

    public static void set(Long userId) { CTX.set(userId); }
    public static Long get() { return CTX.get(); }
    public static void clear() { CTX.remove(); }
}
