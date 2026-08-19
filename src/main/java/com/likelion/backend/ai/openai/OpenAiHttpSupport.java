package com.likelion.backend.ai.openai;

import com.likelion.backend.ai.config.OpenAiProperties;
import com.likelion.backend.common.exception.BusinessException;
import java.net.SocketTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OpenAiHttpSupport {
	private final OpenAiProperties properties;
	public OpenAiHttpSupport(OpenAiProperties properties) { this.properties = properties; }

	public <T> T execute(CheckedSupplier<T> request, String errorCode) {
		if (properties.apiKey() == null || properties.apiKey().isBlank())
			throw new BusinessException("OPENAI_NOT_CONFIGURED", "OpenAI API key is not configured", HttpStatus.SERVICE_UNAVAILABLE);
		RuntimeException last = null;
		for (int attempt = 1; attempt <= Math.max(1, properties.maxAttempts()); attempt++) {
			try { return request.get(); }
			catch (RestClientResponseException ex) {
				last = ex;
				if (ex.getStatusCode().value() != 429 && !ex.getStatusCode().is5xxServerError()) break;
			} catch (ResourceAccessException ex) { last = ex; }
		}
		String code = last instanceof ResourceAccessException && hasTimeout(last) ? "OPENAI_TIMEOUT" : errorCode;
		throw new BusinessException(code, "OpenAI request failed", HttpStatus.BAD_GATEWAY);
	}

	private boolean hasTimeout(Throwable value) {
		for (Throwable current = value; current != null; current = current.getCause())
			if (current instanceof SocketTimeoutException) return true;
		return false;
	}
	@FunctionalInterface public interface CheckedSupplier<T> { T get(); }
}
