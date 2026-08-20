package com.likelion.backend.environment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(EnvironmentApiConfig.Properties.class)
public class EnvironmentApiConfig {

	@Bean("environmentRestTemplate")
	RestTemplate environmentRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(3_000);
		factory.setReadTimeout(15_000);
		return new RestTemplate(factory);
	}

	@ConfigurationProperties(prefix = "environment.api")
	public record Properties(String dataGoKrServiceKey, String kakaoRestApiKey) {
	}
}
