package com.namejm.stream_guppy.config;

import com.namejm.stream_guppy.dto.RestResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RestResult> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        RestResult result = RestResult.failure(e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResult> handleException(Exception e) {
        RestResult result = RestResult.failure(e.getMessage());
        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
