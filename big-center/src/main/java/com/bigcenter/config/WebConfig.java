package com.bigcenter.config;

import com.bigcenter.auth.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 配置（java-core/16-接口设计：/api/** 统一前缀）。
 * JWT 拦截器统一鉴权：/api/auth 的 register/login/refresh、支付回调（渠道服务器无 JWT）、
 * 扫码登录的 create/poll（PC 未登录态，uuid 即一次性凭据）放行；
 * /s/** 短链跳转不走 /api 前缀天然放行。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor authInterceptor;

    public WebConfig(JwtAuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/register", "/api/auth/login", "/api/auth/refresh",
                        "/api/pay/callback", "/api/qrcode/create", "/api/qrcode/*/status");
    }
}
