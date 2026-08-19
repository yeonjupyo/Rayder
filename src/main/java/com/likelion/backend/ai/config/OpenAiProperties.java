package com.likelion.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ai.openai")
public record OpenAiProperties(String apiKey, String baseUrl, String chatModel, String embeddingModel,
	int connectTimeout, int readTimeout, int maxAttempts) {
	public OpenAiProperties {
		baseUrl = baseUrl == null ? "https://api.openai.com" : baseUrl;
		chatModel = chatModel == null ? "gpt-4o-mini" : chatModel;
		embeddingModel = embeddingModel == null ? "text-embedding-3-small" : embeddingModel;
		connectTimeout = connectTimeout <= 0 ? 3000 : connectTimeout;
		readTimeout = readTimeout <= 0 ? 30000 : readTimeout;
		maxAttempts = maxAttempts <= 0 ? 2 : maxAttempts;
	}
}
