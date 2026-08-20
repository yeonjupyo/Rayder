package com.likelion.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 단일 진입점. 병합 이후 코드가 com.likelion.backend(루틴/알림/AI/환경)와
 * com.example.uvmate(진단/스킨몽/홈/챗봇) 두 트리에 나뉘어 있어 두 곳을 모두 스캔한다.
 * 매퍼도 두 트리에서 @Mapper 인터페이스만 골라 등록한다.
 */
@SpringBootApplication(scanBasePackages = {"com.likelion.backend", "com.example.uvmate"})
@MapperScan(basePackages = {"com.likelion.backend", "com.example.uvmate"},
	annotationClass = org.apache.ibatis.annotations.Mapper.class)
@EnableScheduling
public class BackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}
}
