package com.example.uvmate.environment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class EnvironmentExceptionHandler {

    @ExceptionHandler(EnvironmentApiException.class)
    public ResponseEntity<Map<String, String>> handle(EnvironmentApiException e) {
        // 외부 API(기상청/에어코리아) 쪽 문제이므로 502 Bad Gateway로 응답
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", e.getMessage()));
    }
}
