package com.example.learn.config;

import com.example.learn.user.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录校验拦截器（对比 community 的"每接口传 token"——统一上移到拦截器）。
 * ⚠ afterCompletion 必须 UserContext.clear()——Tomcat 线程复用不清理会串用户
 * （并发篇 07 ThreadLocal 泄漏的同族坑）。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redis;

    public AuthInterceptor(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
            return false;
        }
        String userId = redis.opsForValue().get("lc:token:" + token);
        if (userId == null) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"msg\":\"登录已过期\"}");
            return false;
        }
        UserContext.set(Long.parseLong(userId));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();     // ThreadLocal 清理（Tomcat 线程复用防串号）
    }
}
