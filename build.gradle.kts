import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
	java
	id("org.springframework.boot") version "4.0.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.likelion"
version = "0.0.1-SNAPSHOT"
description = "hackathon backend"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

springBoot {
	mainClass.set("com.likelion.backend.BackendApplication")
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Web
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// JDBC + MyBatis (MariaDB)
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.1")
	runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.5.9")
	implementation("org.apache.pdfbox:pdfbox:3.0.5")

	// 비밀번호 해싱만 필요해서 crypto 모듈만 쓴다(Security 필터 체인은 아직 없음).
	implementation("org.springframework.security:spring-security-crypto")

	// Ops / config
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	// Lombok (optional but common with MyBatis DTO/VO boilerplate)
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.mybatis.spring.boot:mybatis-spring-boot-starter-test:4.0.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<BootJar> {
	archiveFileName.set("${project.name}.jar")
}
