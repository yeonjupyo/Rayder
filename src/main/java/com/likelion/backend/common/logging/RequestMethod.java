package com.likelion.backend.common.logging;

/**
 * Mirrors the NestJS {@code RequestMethod} enum used for the `request.method`
 * column. Kept as plain HTTP verbs; unknown/unsupported verbs fall back to
 * {@link #UNKNOWN} rather than throwing during logging.
 */
public enum RequestMethod {
	GET,
	POST,
	PUT,
	PATCH,
	DELETE,
	HEAD,
	OPTIONS,
	UNKNOWN;

	public static RequestMethod from(String httpMethod) {
		if (httpMethod == null) {
			return UNKNOWN;
		}
		try {
			return RequestMethod.valueOf(httpMethod.toUpperCase());
		} catch (IllegalArgumentException ex) {
			return UNKNOWN;
		}
	}
}
