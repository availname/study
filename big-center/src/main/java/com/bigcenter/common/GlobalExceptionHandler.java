package com.bigcenter.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理（java-core/16-接口设计 + 17-日志规范：日志六要素——时间/级别/traceId/位置/内容/异常栈）
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> biz(BizException e) {
        // 业务异常属预期内：warn 级别带 traceId 即可，不打异常栈
        log.warn("traceId={} 业务异常 code={} msg={}", MDC.get("traceId"), e.getErrorCode().code(), e.getMessage());
        return ApiResponse.error(e.getErrorCode().code(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> unknown(Exception e) {
        // 未预期异常：error 级别全栈（六要素之异常栈），traceId 从 MDC 供日志系统聚合
        log.error("traceId={} 未预期异常", MDC.get("traceId"), e);
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR.code(), "系统繁忙: " + e.getMessage());
    }
}
