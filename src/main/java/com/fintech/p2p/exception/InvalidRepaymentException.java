package com.fintech.p2p.exception;

// 无效还款异常
public class InvalidRepaymentException extends RuntimeException {
    public InvalidRepaymentException(String message) {
        super(message);
    }
}
