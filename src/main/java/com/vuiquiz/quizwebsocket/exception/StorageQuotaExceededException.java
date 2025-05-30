package com.vuiquiz.quizwebsocket.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE) // Or HttpStatus.FORBIDDEN (403) or INSUFFICIENT_STORAGE (507)
public class StorageQuotaExceededException extends RuntimeException {
    public StorageQuotaExceededException(String message) {
        super(message);
    }

    public StorageQuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}