package com.likelion.backend.notification.dto;

import com.likelion.backend.notification.domain.NotificationType;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record NotificationSettingRequest(
	@NotNull NotificationType type,
	@NotNull Boolean enabled,
	@NotNull List<@NotNull String> times
) {
}
