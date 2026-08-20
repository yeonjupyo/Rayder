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
public class NotificationWarningSetting {
	private Long userId;
	private boolean enabled;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
