package com.likelion.backend.common.auth;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 개발용 인증 브릿지. 루틴 · 케어메모 · 알림 · AI 엔드포인트는 요청 속성
 * {@code authenticatedUserId} 를 읽는데, 이 값을 채우는 주체가 아직 없어서 호출 자체가 불가능하다.
 * 그래서 프론트 연동을 먼저 진행할 수 있도록 임시로 이 속성만 채운다.
 *
 * <p>토큰을 검증하지 않는다. 사용자를 이렇게 정한다.
 * <ol>
 *   <li>{@code Authorization: Bearer dev.<userId>...} 의 userId</li>
 *   <li>{@code X-Dev-User-Id: <userId>} 헤더</li>
 *   <li>둘 다 없으면 {@code auth.dev.default-user-id}</li>
 * </ol>
 *
 * <p>{@code auth.dev.enabled=true} 일 때만 등록되며 기본값은 false 다. 로컬 프로파일에서만 켠다.
 * 실제 JWT 필터가 들어오면 이 클래스는 삭제 대상이다.
 */
@Component
@ConditionalOnProperty(name = "auth.dev.enabled", havingValue = "true")
@EnableConfigurationProperties(DevAuthenticationFilter.DevAuthProperties.class)
public class DevAuthenticationFilter extends OncePerRequestFilter {

	public static final String AUTHENTICATED_USER_ID = "authenticatedUserId";

	private static final String BEARER_PREFIX = "Bearer ";
	private static final String DEV_TOKEN_PREFIX = "dev.";
	private static final String DEV_USER_ID_HEADER = "X-Dev-User-Id";

	private final DevAuthProperties properties;

	public DevAuthenticationFilter(DevAuthProperties properties) {
		this.properties = properties;
	}

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		if (request.getAttribute(AUTHENTICATED_USER_ID) == null) {
			request.setAttribute(AUTHENTICATED_USER_ID, resolveUserId(request));
		}
		filterChain.doFilter(request, response);
	}

	private long resolveUserId(HttpServletRequest request) {
		Long fromToken = parse(stripDevToken(request.getHeader("Authorization")));
		if (fromToken != null) {
			return fromToken;
		}
		Long fromHeader = parse(request.getHeader(DEV_USER_ID_HEADER));
		return fromHeader != null ? fromHeader : properties.defaultUserId();
	}

	/** {@code Bearer dev.7.<무엇이든>} 형태에서 7 을 꺼낸다. 그 외 형식은 무시한다. */
	private String stripDevToken(String authorization) {
		if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
			return null;
		}
		String token = authorization.substring(BEARER_PREFIX.length()).trim();
		if (!token.startsWith(DEV_TOKEN_PREFIX)) {
			return null;
		}
		String rest = token.substring(DEV_TOKEN_PREFIX.length());
		int separator = rest.indexOf('.');
		return separator < 0 ? rest : rest.substring(0, separator);
	}

	private Long parse(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		try {
			long parsed = Long.parseLong(value.trim());
			return parsed > 0 ? parsed : null;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	@ConfigurationProperties(prefix = "auth.dev")
	public record DevAuthProperties(boolean enabled, Long defaultUserId) {
		public DevAuthProperties {
			defaultUserId = defaultUserId == null ? 1L : defaultUserId;
		}
	}
}
