package com.likelion.backend.notification.service;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion.backend.environment.dto.UvForecastPoint;
import com.likelion.backend.environment.service.EnvironmentQueryService;
import com.likelion.backend.notification.domain.NotificationType;
import com.likelion.backend.notification.mapper.NotificationMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDeliverySchedulerTest {
	@Mock NotificationMapper mapper;
	@Mock EnvironmentQueryService environmentService;
	@Mock PushSender pushSender;
	NotificationDeliveryScheduler scheduler;
	Clock clock;

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(Instant.parse("2026-08-20T01:30:00Z"), ZoneId.of("Asia/Seoul"));
		scheduler = new NotificationDeliveryScheduler(mapper, environmentService, pushSender, clock);
	}

	@Test
	void sendsDueRoutineInSeoulTime() {
		when(mapper.findDueScheduledTargets(LocalTime.of(10, 30))).thenReturn(List.of(
			new NotificationMapper.DeliveryTarget(7L, NotificationType.ROUTINE, "token", null, null)));

		scheduler.sendScheduledNotifications();

		verify(pushSender).send(eq("token"), eq("루틴 알림"), contains("루틴"), anyMap());
	}

	@Test
	void sendsDangerousUvForecastOnlyOncePerForecast() {
		LocalDateTime forecastAt = LocalDateTime.of(2026, 8, 20, 12, 0);
		var target = new NotificationMapper.WarningTarget(7L, "token", "서울특별시", "강남구");
		when(mapper.findWarningTargets()).thenReturn(List.of(target));
		when(environmentService.getUvForecast("서울특별시", "강남구"))
			.thenReturn(List.of(new UvForecastPoint(forecastAt, 7, "높음")));
		when(mapper.insertWarningDeliveryIfAbsent(7L, forecastAt)).thenReturn(1);

		scheduler.sendUvRiskWarnings();

		verify(pushSender).send(eq("token"), eq("자외선 위험 경고"), contains("높음"), anyMap());
	}
}
