package com.likelion.backend.common.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Dedicated thread pool for fire-and-forget work (currently: request log
 * writes). Kept separate from the Tomcat/servlet request-handling threads so
 * logging never contends with actual request processing.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "requestLogExecutor")
	public Executor requestLogExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("request-log-");
		executor.initialize();
		return executor;
	}
}
