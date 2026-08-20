package com.likelion.backend.notification.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSetting {
	private Long notificationId;
	private Long userId;
	private NotificationType type;
	private boolean enabled;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public void changeEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
