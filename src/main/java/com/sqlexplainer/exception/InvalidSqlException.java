package com.sqlexplainer.exception;

import org.springframework.http.HttpStatus;

public class InvalidSqlException extends ApiException {

    public InvalidSqlException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
