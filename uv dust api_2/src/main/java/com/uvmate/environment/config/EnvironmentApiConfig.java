package com.uvmate.environment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class EnvironmentApiConfig {

    // application.yml 의 data-go-kr.service-key 에서 주입됨 (공공데이터포털 공통 인증키)
    @Value("${data-go-kr.service-key}")
    private String serviceKey;

    @Bean
    public RestTemplate environmentRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public String getServiceKey() {
        return serviceKey;
    }
}
