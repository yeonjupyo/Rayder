package com.example.uvmate.environment.exception;

/**
 * 기상청/에어코리아 API 호출 실패, 파싱 실패, 키 만료 등의 상황에서 던지는 공통 예외.
 * 컨트롤러 단에서 이 예외 하나만 잡으면 되도록 통일.
 */
public class EnvironmentApiException extends RuntimeException {

    public EnvironmentApiException(String message) {
        super(message);
    }

    public EnvironmentApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
