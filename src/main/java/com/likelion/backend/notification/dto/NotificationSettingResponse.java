package com.likelion.backend.notification.dto;

import com.likelion.backend.notification.domain.NotificationType;
import java.time.LocalDateTime;
import java.util.List;

public record NotificationSettingResponse(
	Long notificationId,
	NotificationType type,
	boolean enabled,
	List<String> times,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
