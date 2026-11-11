package com.bigcenter.rate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解（java-core/12 题三）：打在 Controller 方法上，AOP 环绕执行 Lua 滑动窗口。
 * 维度：key 按"接口+用户"细限（登录用户），未登录接口可改用 IP（@RateLimit(byIp=true)）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 窗口内最大请求数 */
    int limit() default 100;

    /** 窗口大小（毫秒），默认 60s */
    long windowMs() default 60_000;
}
