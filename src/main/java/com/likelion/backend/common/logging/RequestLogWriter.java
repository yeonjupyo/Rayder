package com.likelion.backend.common.logging;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists a single request/response log row. Runs on a separate thread via
 * {@link Async} (see {@code AsyncConfig}) so a slow DB write never adds
 * latency to the request it's logging — mirrors the NestJS version firing
 * {@code prisma.request.create(...)} without awaiting it.
 *
 * <p>Failures here are swallowed (logged only): losing a log row must never
 * fail or slow down the request itself.
 */
@Component
@RequiredArgsConstructor
public class RequestLogWriter {

	private static final Logger log = LoggerFactory.getLogger(RequestLogWriter.class);

	private final RequestLogMapper requestLogMapper;

	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void write(RequestLog.RequestLogBuilder builder) {
		try {
			LocalDateTime now = LocalDateTime.now();
			RequestLog requestLog = builder
				.uuid(UUID.randomUUID().toString())
				.requestAt(now)
				.responseAt(now)
				.createdAt(now)
				.updatedAt(now)
				.build();
			requestLogMapper.insert(requestLog);
		} catch (Exception ex) {
			log.error("Failed to persist request log", ex);
		}
	}
}
