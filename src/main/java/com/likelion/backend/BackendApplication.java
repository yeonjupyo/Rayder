package com.likelion.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 단일 진입점. 기능별 하위 패키지(auth, diagnosis, skinmon, home, chat, routine,
 * notification, ai, environment)를 모두 스캔하고, 매퍼는 @Mapper 인터페이스만 골라 등록한다.
 */
@SpringBootApplication
@MapperScan(basePackages = "com.likelion.backend",
	annotationClass = org.apache.ibatis.annotations.Mapper.class)
@EnableScheduling
public class BackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}
}
