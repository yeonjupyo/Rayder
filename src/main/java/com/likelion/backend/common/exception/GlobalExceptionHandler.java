package com.likelion.backend.common.exception;

import com.likelion.backend.common.logging.ClientIpResolver;
import com.likelion.backend.common.logging.JsonLogSupport;
import com.likelion.backend.common.logging.RequestLog;
import com.likelion.backend.common.logging.RequestLogWriter;
import com.likelion.backend.common.logging.RequestMethod;
import com.likelion.backend.common.logging.UserAgentParser;
import com.likelion.backend.common.response.ApiErrorBody;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions thrown anywhere in the request pipeline into a
 * consistent {@link ErrorResponse} JSON body, and persists a request-log row
 * for the failed request — ported from the NestJS {@code
 * GlobalExceptionFilter}, which wrote the error request to the DB on every
 * caught exception before responding to the client.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final RequestLogWriter requestLogWriter;
	private final JsonLogSupport jsonLogSupport;

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(
		BusinessException ex, HttpServletRequest request
	) {
		logFailedRequest(request, ex.getStatus().value(), ex.getMessage());
		ErrorResponse body = ErrorResponse.of(
			ex.getStatus().value(), ex.getCode(), ex.getMessage(), request.getRequestURI()
		);
		return ResponseEntity.status(ex.getStatus()).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
		MethodArgumentNotValidException ex, HttpServletRequest request
	) {
		String message = ex.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.orElse("Invalid request");
		logFailedRequest(request, HttpStatus.BAD_REQUEST.value(), message);
		ErrorResponse body = ErrorResponse.of(
			HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", message, request.getRequestURI()
		);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpectedException(
		Exception ex, HttpServletRequest request
	) {
		logFailedRequest(request, HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage());
		ErrorResponse body = ErrorResponse.of(
			HttpStatus.INTERNAL_SERVER_ERROR.value(),
			"INTERNAL_SERVER_ERROR",
			"Unexpected error occurred",
			request.getRequestURI()
		);
		return ResponseEntity.internalServerError().body(body);
	}

	/**
	 * Persists the failed request. Body/query/params are read best-effort;
	 * on the exception path the request body stream may already be
	 * consumed or unavailable, so failures here are swallowed by
	 * {@link RequestLogWriter} itself.
	 */
	private void logFailedRequest(HttpServletRequest request, int status, String message) {
		List<String> errors = List.of(message != null ? message : "Unknown error");
		ApiErrorBody responseBody = ApiErrorBody.of(errors);

		RequestLog.RequestLogBuilder builder = RequestLog.builder()
			.ip(ClientIpResolver.resolve(request))
			.userAgent(jsonLogSupport.toJson(UserAgentParser.parse(request.getHeader("User-Agent"))))
			.method(RequestMethod.from(request.getMethod()))
			.host(request.getServerName())
			.url(request.getRequestURI())
			.body(null)
			.query(request.getQueryString())
			.params(null)
			.headers(jsonLogSupport.redactHeaders(headersToMap(request)))
			.cookies(null)
			.responseBody(jsonLogSupport.toJson(responseBody))
			.error(message)
			.status(status)
			.duration(null);

		requestLogWriter.write(builder);
	}

	private Map<String, String> headersToMap(HttpServletRequest request) {
		Map<String, String> headers = new LinkedHashMap<>();
		Enumeration<String> names = request.getHeaderNames();
		if (names == null) {
			return headers;
		}
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			headers.put(name, request.getHeader(name));
		}
		return headers;
	}
}
