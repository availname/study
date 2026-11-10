package com.example.mqmall.common;

/** 业务异常（用户可见的错误，如库存不足） */
public class BizException extends RuntimeException {
    public BizException(String msg) { super(msg); }
}
