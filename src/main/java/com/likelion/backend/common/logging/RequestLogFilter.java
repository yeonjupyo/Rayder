package com.likelion.backend.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Ported from the NestJS {@code LoggingInterceptor}.
 *
 * <p>Spring has no direct equivalent of Nest's interceptor (which can both
 * wrap the request and observe the emitted response body in one place), so
 * this uses the standard servlet approach instead: wrap the request/response
 * in content-caching wrappers so the bodies can be read after the handler
 * runs, without consuming the streams the handler itself needs.
 *
 * <p>Only successful (2xx) responses are logged here, matching the original
 * behavior; error responses are logged separately by
 * {@link com.likelion.backend.common.exception.GlobalExceptionHandler}, since
 * exceptions never reach this filter's "after handler" branch on their normal
 * path — Spring's exception resolution runs before the response is
 * committed, but after this filter's early half executes.
 */
@Component
@ConditionalOnProperty(prefix = "logging.request", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RequestLogFilter extends OncePerRequestFilter {

	/** Caps how much of the request body is buffered in memory for logging. */
	private static final int MAX_CACHED_BODY_BYTES = 1024 * 1024;

	private final RequestLogWriter requestLogWriter;
	private final JsonLogSupport jsonLogSupport;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		ContentCachingRequestWrapper wrappedRequest =
			new ContentCachingRequestWrapper(request, MAX_CACHED_BODY_BYTES);
		ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
		long startTime = System.currentTimeMillis();

		try {
			filterChain.doFilter(wrappedRequest, wrappedResponse);
		} finally {
			int status = wrappedResponse.getStatus();
			if (status >= 200 && status < 300) {
				logSuccess(wrappedRequest, wrappedResponse, status, startTime);
			}
			// Must run after we're done reading, or the client never gets the body.
			wrappedResponse.copyBodyToResponse();
		}
	}

	private void logSuccess(
		ContentCachingRequestWrapper request,
		ContentCachingResponseWrapper response,
		int status,
		long startTime
	) {
		String requestBody = readBody(request.getContentAsByteArray());
		String responseBody = readBody(response.getContentAsByteArray());

		RequestLog.RequestLogBuilder builder = RequestLog.builder()
			.ip(ClientIpResolver.resolve(request))
			.userAgent(jsonLogSupport.toJson(UserAgentParser.parse(request.getHeader("User-Agent"))))
			.method(RequestMethod.from(request.getMethod()))
			.host(request.getServerName())
			.url(request.getRequestURI())
			.body(requestBody != null ? jsonLogSupport.toJson(requestBody) : null)
			.query(request.getQueryString() != null ? jsonLogSupport.toJson(request.getQueryString()) : null)
			.params(null) // Spring MVC has no direct equivalent of Fastify's path-param map at filter level.
			.headers(jsonLogSupport.redactHeaders(headersToMap(request)))
			.cookies(cookiesToJson(request))
			.responseBody(responseBody)
			.error(null)
			.status(status)
			.duration((int) (System.currentTimeMillis() - startTime));

		requestLogWriter.write(builder);
	}

	private String readBody(byte[] content) {
		if (content == null || content.length == 0) {
			return null;
		}
		return new String(content, StandardCharsets.UTF_8);
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

	private String cookiesToJson(HttpServletRequest request) {
		if (request.getCookies() == null || request.getCookies().length == 0) {
			return null;
		}
		Map<String, String> cookies = new LinkedHashMap<>();
		for (var cookie : request.getCookies()) {
			cookies.put(cookie.getName(), cookie.getValue());
		}
		return jsonLogSupport.toJson(cookies);
	}
}
