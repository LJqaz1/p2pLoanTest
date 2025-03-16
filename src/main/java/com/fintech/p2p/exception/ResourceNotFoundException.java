package com.fintech.p2p.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class ResourceNotFoundException extends RuntimeException {
    // 资源未找到异常
    public ResourceNotFoundException(String message) {
        super(message);
    }
}