package com.likelion.backend.notification.service;

import com.likelion.backend.environment.dto.EnvironmentInfo;
import com.likelion.backend.environment.dto.UvForecastPoint;
import com.likelion.backend.environment.service.EnvironmentQueryService;
import com.likelion.backend.notification.domain.NotificationType;
import com.likelion.backend.notification.mapper.NotificationMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.expo.enabled", havingValue = "true")
public class NotificationDeliveryScheduler {
	private final NotificationMapper mapper;
	private final EnvironmentQueryService environmentService;
	private final PushSender pushSender;
	private final Clock notificationClock;

	@Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
	public void sendScheduledNotifications() {
		LocalTime time = LocalTime.now(notificationClock).truncatedTo(ChronoUnit.MINUTES);
		for (var target : mapper.findDueScheduledTargets(time)) {
			try { sendScheduled(target); }
			catch (RuntimeException exception) { log.error("Scheduled notification failed for user {}", target.userId(), exception); }
		}
	}

	@Scheduled(cron = "0 5 */3 * * *", zone = "Asia/Seoul")
	public void sendUvRiskWarnings() {
		var targetsByUser = mapper.findWarningTargets().stream()
			.collect(Collectors.groupingBy(NotificationMapper.WarningTarget::userId));
		LocalDateTime now = LocalDateTime.now(notificationClock);
		for (List<NotificationMapper.WarningTarget> targets : targetsByUser.values()) {
			var target = targets.get(0);
			try {
				UvForecastPoint risk = environmentService.getUvForecast(target.sido(), target.gugun()).stream()
					.filter(point -> !point.forecastAt().isBefore(now.minusHours(3)))
					.filter(point -> !point.forecastAt().isAfter(now.plusHours(3)))
					.filter(UvForecastPoint::isDangerous).findFirst().orElse(null);
				if (risk != null && mapper.insertWarningDeliveryIfAbsent(target.userId(), risk.forecastAt()) == 1) {
					for (var device : targets) pushSender.send(device.token(), "자외선 위험 경고",
						"현재 또는 가까운 시간의 자외선 단계가 " + risk.level() + "입니다.",
						Map.of("type", "UV_RISK_WARNING", "value", String.valueOf(risk.value())));
				}
			} catch (RuntimeException exception) { log.error("UV warning failed for user {}", target.userId(), exception); }
		}
	}

	private void sendScheduled(NotificationMapper.DeliveryTarget target) {
		if (target.type() == NotificationType.ROUTINE) {
			pushSender.send(target.token(), "루틴 알림", "설정한 스킨케어 루틴을 확인해 주세요.",
				Map.of("type", "ROUTINE"));
			return;
		}
		if (target.sido() == null || target.gugun() == null) return;
		if (target.type() == NotificationType.UV) {
			EnvironmentInfo uv = environmentService.getUv(target.sido(), target.gugun());
			pushSender.send(target.token(), "자외선 알림", "자외선 지수 " + uv.value() + " (" + uv.level() + ")",
				Map.of("type", "UV", "value", String.valueOf(uv.value()), "level", uv.level()));
		} else {
			List<EnvironmentInfo> dust = environmentService.getDust(target.sido(), target.gugun());
			pushSender.send(target.token(), "미세먼지 알림",
				"PM10 " + dust.get(0).value() + " (" + dust.get(0).level() + "), PM2.5 " + dust.get(1).value() + " (" + dust.get(1).level() + ")",
				Map.of("type", "DUST"));
		}
	}
}
