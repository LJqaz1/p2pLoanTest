package com.fintech.p2p.exception;

/**
 * 邮件发送异常
 */
public class EmailSendingException extends RuntimeException {
    public EmailSendingException(String message) {
        super(message);
    }

    public EmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}