package com.bigcenter.common;

/** 业务异常：携带错误码段，由 GlobalExceptionHandler 统一转为 ApiResponse（java-core/16） */
public class BizException extends RuntimeException {
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.msg());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String detail) {
        super(errorCode.msg() + ": " + detail);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
