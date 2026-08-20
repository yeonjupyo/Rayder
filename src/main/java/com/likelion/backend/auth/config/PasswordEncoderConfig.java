package com.likelion.backend.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * spring-security-crypto 만 쓰고 Spring Security 전체는 넣지 않았다. 필터 체인이 필요해지면
 * (실제 JWT 인증) 그때 starter 로 올리면 된다.
 */
@Configuration
public class PasswordEncoderConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
