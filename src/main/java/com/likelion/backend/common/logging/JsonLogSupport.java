package com.likelion.backend.common.logging;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared JSON serialization for the request logger and global exception
 * filter, plus the header/cookie redaction the NestJS version applied
 * inline. Kept as one bean so both call sites use the same ObjectMapper
 * (and the same sensitive-header list) instead of drifting apart.
 */
@Component
public class JsonLogSupport {

	private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "cookie");

	private final ObjectMapper objectMapper;

	public JsonLogSupport(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * Serializes an arbitrary value to a JSON string for a MariaDB JSON
	 * column. Returns {@code null} on null input or serialization failure
	 * rather than throwing — logging must never break the request it's
	 * observing.
	 */
	public String toJson(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JacksonException ex) {
			return null;
		}
	}

	public String redactHeaders(Map<String, String> headers) {
		Map<String, String> filtered = headers.entrySet().stream()
			.filter(entry -> !SENSITIVE_HEADERS.contains(entry.getKey().toLowerCase()))
			.collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		return toJson(filtered);
	}
}
