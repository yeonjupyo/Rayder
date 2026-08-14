package com.likelion.backend.common.response;

import java.time.Instant;
import java.util.List;

/**
 * Response envelope matching the NestJS `GlobalExceptionFilter` error
 * payload shape: {@code { success, error, data, requested_at } }.
 * Kept separate from {@code ErrorResponse} (used elsewhere in this repo's
 * exception handling) so existing handlers aren't forced to switch shape;
 * this is opt-in for endpoints that need parity with the old API contract.
 */
public record ApiErrorBody(
	boolean success,
	List<String> error,
	Object data,
	String requestedAt
) {
	public static ApiErrorBody of(List<String> error) {
		return new ApiErrorBody(false, error, null, Instant.now().toString());
	}
}
