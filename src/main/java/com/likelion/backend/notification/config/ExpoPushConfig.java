package com.likelion.backend.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(ExpoPushConfig.Properties.class)
public class ExpoPushConfig {
	@Bean("expoPushRestTemplate")
	RestTemplate expoPushRestTemplate() {
		var factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(3_000);
		factory.setReadTimeout(10_000);
		return new RestTemplate(factory);
	}

	@ConfigurationProperties(prefix = "notification.expo")
	public record Properties(boolean enabled, String accessToken, String sendUrl, String receiptsUrl) { }
}
