package com.likelion.backend.ai.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({OpenAiProperties.class, RagProperties.class})
public class AiConfig {
	@Bean("openAiRestClient")
	RestClient openAiRestClient(OpenAiProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofMillis(properties.connectTimeout()));
		factory.setReadTimeout(Duration.ofMillis(properties.readTimeout()));
		RestClient.Builder builder = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory);
		if (properties.apiKey() != null && !properties.apiKey().isBlank())
			builder.defaultHeader("Authorization", "Bearer " + properties.apiKey());
		return builder.build();
	}
}
