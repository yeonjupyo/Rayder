package com.likelion.backend.common.logging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestLogWriterTest {

	@Mock RequestLogMapper requestLogMapper;

	@Test
	void writesNothingWhenDisabled() {
		new RequestLogWriter(requestLogMapper, false).write(RequestLog.builder());

		verify(requestLogMapper, never()).insert(any());
	}

	@Test
	void writesWhenEnabled() {
		new RequestLogWriter(requestLogMapper, true).write(RequestLog.builder());

		verify(requestLogMapper).insert(any());
	}
}
