package com.likelion.backend.common.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;

/**
 * Minimal User-Agent parser, standing in for the Node {@code useragent}
 * package. Only extracts what the original NestJS logging code stored
 * (browser family/version, OS family/version) — good enough for log
 * analytics without pulling in a full UA-parsing dependency.
 */
public final class UserAgentParser {

	private static final Pattern OS_WINDOWS = Pattern.compile("Windows NT ([0-9.]+)");
	private static final Pattern OS_MAC = Pattern.compile("Mac OS X ([0-9_.]+)");
	private static final Pattern OS_ANDROID = Pattern.compile("Android ([0-9.]+)");
	private static final Pattern OS_IOS = Pattern.compile("(?:iPhone|iPad).*OS ([0-9_]+)");

	private static final Pattern BROWSER_EDGE = Pattern.compile("Edg/([0-9.]+)");
	private static final Pattern BROWSER_CHROME = Pattern.compile("Chrome/([0-9.]+)");
	private static final Pattern BROWSER_FIREFOX = Pattern.compile("Firefox/([0-9.]+)");
	private static final Pattern BROWSER_SAFARI = Pattern.compile("Version/([0-9.]+).*Safari");

	private UserAgentParser() {
	}

	public static ParsedUserAgent parse(String rawUserAgent) {
		if (rawUserAgent == null || rawUserAgent.isBlank()) {
			return ParsedUserAgent.builder()
				.raw("unknown")
				.family("unknown")
				.version("unknown")
				.os("unknown")
				.build();
		}

		return ParsedUserAgent.builder()
			.raw(rawUserAgent)
			.family(matchBrowserFamily(rawUserAgent))
			.version(matchBrowserVersion(rawUserAgent))
			.os(matchOs(rawUserAgent))
			.build();
	}

	private static String matchBrowserFamily(String ua) {
		if (BROWSER_EDGE.matcher(ua).find()) {
			return "Edge";
		}
		if (ua.contains("Chrome") && !ua.contains("Chromium")) {
			return "Chrome";
		}
		if (BROWSER_FIREFOX.matcher(ua).find()) {
			return "Firefox";
		}
		if (ua.contains("Safari") && !ua.contains("Chrome")) {
			return "Safari";
		}
		return "Other";
	}

	private static String matchBrowserVersion(String ua) {
		Matcher matcher = switch (matchBrowserFamily(ua)) {
			case "Edge" -> BROWSER_EDGE.matcher(ua);
			case "Chrome" -> BROWSER_CHROME.matcher(ua);
			case "Firefox" -> BROWSER_FIREFOX.matcher(ua);
			case "Safari" -> BROWSER_SAFARI.matcher(ua);
			default -> null;
		};
		if (matcher != null && matcher.find()) {
			return matcher.group(1);
		}
		return "0.0";
	}

	private static String matchOs(String ua) {
		if (OS_WINDOWS.matcher(ua).find()) {
			return "Windows";
		}
		if (OS_MAC.matcher(ua).find()) {
			return "macOS";
		}
		if (OS_ANDROID.matcher(ua).find()) {
			return "Android";
		}
		if (OS_IOS.matcher(ua).find()) {
			return "iOS";
		}
		if (ua.contains("Linux")) {
			return "Linux";
		}
		return "Other";
	}

	@Getter
	@Builder
	public static class ParsedUserAgent {
		private String raw;
		private String family;
		private String version;
		private String os;
	}
}
