package com.likelion.backend.environment.exception;

import com.likelion.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class EnvironmentApiException extends BusinessException {
	public EnvironmentApiException(String message) {
		this("ENVIRONMENT_UPSTREAM_ERROR", message, HttpStatus.BAD_GATEWAY, null);
	}

	public EnvironmentApiException(String message, Throwable cause) {
		this("ENVIRONMENT_UPSTREAM_ERROR", message, HttpStatus.BAD_GATEWAY, cause);
	}

	private EnvironmentApiException(String code, String message, HttpStatus status, Throwable cause) {
		super(code, message, status);
		if (cause != null) initCause(cause);
	}

	public static EnvironmentApiException invalidInput(String message) {
		return new EnvironmentApiException("INVALID_ENVIRONMENT_REQUEST", message, HttpStatus.BAD_REQUEST, null);
	}

	public static EnvironmentApiException regionNotFound(String message) {
		return new EnvironmentApiException("REGION_NOT_FOUND", message, HttpStatus.BAD_REQUEST, null);
	}
}
