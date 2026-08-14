package com.likelion.backend.common.exception;

import java.time.Instant;

/**
 * Uniform error payload returned by the API on failure.
 */
public record ErrorResponse(
	Instant timestamp,
	int status,
	String code,
	String message,
	String path
) {
	public static ErrorResponse of(int status, String code, String message, String path) {
		return new ErrorResponse(Instant.now(), status, code, message, path);
	}
}
