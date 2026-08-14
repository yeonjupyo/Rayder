package com.likelion.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for domain/business rule violations.
 * Thrown from service layer; translated to an HTTP response by
 * {@link com.likelion.backend.common.exception.GlobalExceptionHandler}.
 */
public class BusinessException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public BusinessException(String code, String message) {
		this(code, message, HttpStatus.BAD_REQUEST);
	}

	public BusinessException(String code, String message, HttpStatus status) {
		super(message);
		this.code = code;
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
