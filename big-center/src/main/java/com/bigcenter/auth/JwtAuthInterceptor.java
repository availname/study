package com.bigcenter.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 统一鉴权拦截器（java-core/19：与 learn-center 的 Redis token 拦截器同构，
 * 差异只在"Redis 查 userId"换成"验签解析 subject"）。
 * 三件套纪律：preHandle 校验 → ThreadLocal 传递 → afterCompletion 清理（线程复用防串号）。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtService jwt;

    public JwtAuthInterceptor(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String auth = request.getHeader("Authorization");
        // ① 必须是 Bearer 头（19 篇：❌ URL 传参会被日志/Referer 泄漏）
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(response, 1004, "未登录或令牌格式错误");
        }
        String token = auth.substring(7);
        Long userId = jwt.verifyAccess(token);
        if (userId == null) {
            return reject(response, 1004, "令牌无效或已过期");
        }
        UserContext.set(userId);
        return true;
    }

    private boolean reject(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}");
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();     // ThreadLocal 清理（Tomcat 线程复用防串号）
    }
}
