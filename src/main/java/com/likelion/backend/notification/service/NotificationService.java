package com.likelion.backend.notification.service;

import com.likelion.backend.common.exception.BusinessException;
import com.likelion.backend.notification.domain.NotificationSetting;
import com.likelion.backend.notification.domain.NotificationType;
import com.likelion.backend.notification.domain.NotificationWarningSetting;
import com.likelion.backend.notification.dto.NotificationListResponse;
import com.likelion.backend.notification.dto.NotificationSettingRequest;
import com.likelion.backend.notification.dto.NotificationSettingResponse;
import com.likelion.backend.notification.dto.NotificationUpdateRequest;
import com.likelion.backend.notification.dto.WarningSettingResponse;
import com.likelion.backend.notification.dto.DeviceTokenRequest;
import com.likelion.backend.notification.dto.NotificationLocationRequest;
import com.likelion.backend.notification.dto.NotificationLocationResponse;
import com.likelion.backend.environment.client.RegionResolver;
import com.likelion.backend.notification.mapper.NotificationMapper;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
		.withResolverStyle(ResolverStyle.STRICT);
	private final NotificationMapper notificationMapper;
	private final RegionResolver regionResolver;

	public NotificationListResponse findAll(long userId) {
		validateUser(userId);
		Map<NotificationType, NotificationSetting> settingsByType = new EnumMap<>(NotificationType.class);
		notificationMapper.findAllByUserId(userId)
			.forEach(setting -> settingsByType.put(setting.getType(), setting));
		List<NotificationSettingResponse> settings = List.of(NotificationType.values()).stream()
			.map(type -> {
				NotificationSetting setting = settingsByType.get(type);
				return setting == null ? defaultResponse(type) : toResponse(setting);
			})
			.toList();
		WarningSettingResponse warning = notificationMapper.findWarningByUserId(userId)
			.map(this::toWarningResponse)
			.orElse(new WarningSettingResponse(false, null, null));
		return new NotificationListResponse(settings, warning);
	}

	@Transactional
	public NotificationSettingResponse create(long userId, NotificationSettingRequest request) {
		validateUser(userId);
		if (notificationMapper.findByUserIdAndType(userId, request.type()).isPresent()) {
			throw new BusinessException("NOTIFICATION_ALREADY_EXISTS",
				"Notification setting already exists for type " + request.type(), HttpStatus.CONFLICT);
		}
		List<LocalTime> times = parseTimes(request.enabled(), request.times());
		NotificationSetting setting = NotificationSetting.builder().userId(userId)
			.type(request.type()).enabled(request.enabled()).build();
		notificationMapper.insertSetting(setting);
		insertTimes(setting.getNotificationId(), times);
		return findOwned(setting.getNotificationId(), userId);
	}

	@Transactional
	public NotificationSettingResponse update(long notificationId, long userId,
		NotificationUpdateRequest request) {
		NotificationSetting setting = requireOwned(notificationId, userId);
		List<LocalTime> times = parseTimes(request.enabled(), request.times());
		setting.changeEnabled(request.enabled());
		notificationMapper.updateSetting(setting);
		notificationMapper.deleteTimes(notificationId);
		insertTimes(notificationId, times);
		return findOwned(notificationId, userId);
	}

	@Transactional
	public void delete(long notificationId, long userId) {
		requireOwned(notificationId, userId);
		notificationMapper.deleteSetting(notificationId);
	}

	@Transactional
	public WarningSettingResponse updateWarning(long userId, boolean enabled) {
		validateUser(userId);
		notificationMapper.upsertWarning(userId, enabled);
		return notificationMapper.findWarningByUserId(userId).map(this::toWarningResponse)
			.orElseThrow(() -> new BusinessException("WARNING_SETTING_SAVE_FAILED",
				"Warning setting could not be saved", HttpStatus.INTERNAL_SERVER_ERROR));
	}

	@Transactional
	public void registerDevice(long userId, DeviceTokenRequest request) {
		validateUser(userId);
		notificationMapper.upsertDeviceToken(userId, request.token(), request.platform());
	}

	@Transactional
	public void unregisterDevice(long userId, String token) {
		validateUser(userId);
		notificationMapper.deactivateDeviceToken(userId, token);
	}

	@Transactional
	public NotificationLocationResponse updateLocation(long userId, NotificationLocationRequest request) {
		validateUser(userId);
		var region = regionResolver.resolve(request.sido(), request.gugun());
		notificationMapper.upsertLocation(userId, region.sido(), region.gugun());
		return new NotificationLocationResponse(region.sido(), region.gugun());
	}

	public NotificationLocationResponse findLocation(long userId) {
		validateUser(userId);
		return notificationMapper.findLocation(userId)
			.map(location -> new NotificationLocationResponse(location.sido(), location.gugun()))
			.orElse(null);
	}

	private NotificationSetting requireOwned(long notificationId, long userId) {
		NotificationSetting setting = notificationMapper.findById(notificationId)
			.orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND",
				"Notification setting not found: " + notificationId, HttpStatus.NOT_FOUND));
		if (!setting.getUserId().equals(userId)) {
			throw new BusinessException("NOTIFICATION_FORBIDDEN",
				"Notification setting belongs to another user", HttpStatus.FORBIDDEN);
		}
		return setting;
	}

	private NotificationSettingResponse findOwned(long notificationId, long userId) {
		return toResponse(requireOwned(notificationId, userId));
	}

	private NotificationSettingResponse toResponse(NotificationSetting setting) {
		List<String> times = notificationMapper.findTimes(setting.getNotificationId()).stream()
			.map(TIME_FORMAT::format).toList();
		return new NotificationSettingResponse(setting.getNotificationId(), setting.getType(),
			setting.isEnabled(), times, setting.getCreatedAt(), setting.getUpdatedAt());
	}

	private NotificationSettingResponse defaultResponse(NotificationType type) {
		return new NotificationSettingResponse(null, type, false, List.of(), null, null);
	}

	private WarningSettingResponse toWarningResponse(NotificationWarningSetting setting) {
		return new WarningSettingResponse(setting.isEnabled(), setting.getCreatedAt(), setting.getUpdatedAt());
	}

	private List<LocalTime> parseTimes(boolean enabled, List<String> values) {
		if (enabled && values.isEmpty()) {
			throw new BusinessException("NOTIFICATION_TIME_REQUIRED",
				"At least one notification time is required when enabled", HttpStatus.BAD_REQUEST);
		}
		List<LocalTime> times;
		try {
			times = values.stream().map(value -> LocalTime.parse(value, TIME_FORMAT)).sorted().toList();
		} catch (DateTimeParseException exception) {
			throw new BusinessException("INVALID_NOTIFICATION_TIME",
				"Time must use HH:mm format", HttpStatus.BAD_REQUEST);
		}
		if (new HashSet<>(times).size() != times.size()) {
			throw new BusinessException("DUPLICATE_NOTIFICATION_TIME",
				"Duplicate notification times are not allowed", HttpStatus.BAD_REQUEST);
		}
		return times;
	}

	private void insertTimes(long notificationId, List<LocalTime> times) {
		if (!times.isEmpty()) {
			notificationMapper.insertTimes(notificationId, times);
		}
	}

	private void validateUser(long userId) {
		if (!notificationMapper.existsUser(userId)) {
			throw new BusinessException("USER_NOT_FOUND", "User not found: " + userId, HttpStatus.NOT_FOUND);
		}
	}
}
