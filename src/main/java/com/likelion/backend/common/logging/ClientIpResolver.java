package com.likelion.backend.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * Resolves the "real" client IP behind proxies/tunnels, mirroring the
 * NestJS {@code getClientIp} plugin. Cloudflare Tunnel injects
 * {@code CF-Connecting-IP}, which takes priority since it's the header
 * Cloudflare itself sets (harder to spoof than client-supplied
 * X-Forwarded-For).
 */
public final class ClientIpResolver {

	private ClientIpResolver() {
	}

	public static String resolve(HttpServletRequest request) {
		String cfConnectingIp = request.getHeader("CF-Connecting-IP");
		if (StringUtils.hasText(cfConnectingIp)) {
			return cfConnectingIp.trim();
		}

		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(forwardedFor)) {
			// X-Forwarded-For can be a comma-separated chain; the first entry
			// is the original client.
			return forwardedFor.split(",")[0].trim();
		}

		String realIp = request.getHeader("X-Real-IP");
		if (StringUtils.hasText(realIp)) {
			return realIp.trim();
		}

		return request.getRemoteAddr();
	}
}
