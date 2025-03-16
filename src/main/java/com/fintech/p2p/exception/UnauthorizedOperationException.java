package com.fintech.p2p.exception;

// 未授权操作异常
public class UnauthorizedOperationException extends RuntimeException {
    public UnauthorizedOperationException(String message) {
        super(message);
    }
}
