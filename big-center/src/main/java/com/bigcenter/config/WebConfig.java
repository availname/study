package com.bigcenter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置（java-core/16-接口设计：/api/** 统一前缀）。
 * 第 2 步接入 JWT AuthInterceptor；第 8 步接入 @RateLimit AOP。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 骨架阶段无拦截器；JWT 步骤在此注册 authInterceptor（放行 /api/auth/**、/s/** 短链跳转）
}
