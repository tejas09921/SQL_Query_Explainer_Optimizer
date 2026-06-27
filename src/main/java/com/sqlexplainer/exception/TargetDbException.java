package com.sqlexplainer.exception;

import org.springframework.http.HttpStatus;

public class TargetDbException extends ApiException {

    public TargetDbException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, cause);
    }
}
