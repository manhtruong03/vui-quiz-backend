// src/main/java/com/vuiquiz/quizwebsocket/exception/UnauthorizedException.java
package com.vuiquiz.quizwebsocket.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED) // Or FORBIDDEN (403) if it's more about permissions than just not being logged in
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}