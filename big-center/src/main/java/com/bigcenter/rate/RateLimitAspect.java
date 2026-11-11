package com.bigcenter.rate;

import com.bigcenter.auth.UserContext;
import com.bigcenter.common.BizException;
import com.bigcenter.common.ErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @RateLimit AOP 环绕（java-core/12 题三落地）：
 * 限流 key = 接口签名 + 登录用户（细限到人）；
 * 超限抛 5001（GlobalExceptionHandler 统一转响应；HTTP 层语义=429 Too Many Requests）。
 */
@Aspect
@Component
public class RateLimitAspect {
    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final RateLimiterService limiter;

    public RateLimitAspect(RateLimiterService limiter) {
        this.limiter = limiter;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String signature = pjp.getSignature().toShortString();      // 如 PaymentService.createPay
        Long userId = UserContext.get();
        // 维度：接口+用户（未登录上下文用 anonymous——公开接口按 IP 的场景见 12 篇追问）
        String key = signature + ":" + (userId == null ? "anonymous" : userId);
        if (!limiter.tryAcquire(key, rateLimit.limit(), rateLimit.windowMs())) {
            log.warn("限流触发 key={} limit={}/{}ms", key, rateLimit.limit(), rateLimit.windowMs());
            throw new BizException(ErrorCode.RATE_LIMITED);         // 5001
        }
        return pjp.proceed();
    }
}
