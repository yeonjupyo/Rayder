package com.likelion.backend.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 프론트엔드(Vite dev server)가 다른 오리진에서 API 를 호출하므로 CORS 허용이 필요하다.
 * 허용 오리진은 web.cors.allowed-origins 로 환경별로 지정한다.
 */
@Configuration
@EnableConfigurationProperties(WebCorsConfig.CorsProperties.class)
public class WebCorsConfig implements WebMvcConfigurer {

	private final CorsProperties properties;

	public WebCorsConfig(CorsProperties properties) {
		this.properties = properties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		if (properties.allowedOrigins().isEmpty()) {
			return;
		}
		registry.addMapping("/api/**")
			.allowedOrigins(properties.allowedOrigins().toArray(String[]::new))
			.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
			.allowedHeaders("*")
			.maxAge(3600);
	}

	@ConfigurationProperties(prefix = "web.cors")
	public record CorsProperties(List<String> allowedOrigins) {
		public CorsProperties {
			allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
		}
	}
}
