package com.likelion.backend.environment.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.validation.annotation.Validated;
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

	/**
	 * 키가 비어 있으면 기동은 성공하고 나중에 공공 API 가 "서비스키 오류" 를 줄 때서야 502 로 드러난다.
	 * 그래서 기동 시점에 막는다.
	 */
	@Validated
	@ConfigurationProperties(prefix = "environment.api")
	public record Properties(
		@NotBlank(message = "environment.api.data-go-kr-service-key 가 필요하다 (환경변수 DATA_GO_KR_SERVICE_KEY)")
		String dataGoKrServiceKey,
		@NotBlank(message = "environment.api.kakao-rest-api-key 가 필요하다 (환경변수 KAKAO_REST_API_KEY)")
		String kakaoRestApiKey) {
	}
}
