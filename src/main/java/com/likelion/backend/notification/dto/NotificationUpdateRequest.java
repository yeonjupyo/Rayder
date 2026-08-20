package com.likelion.backend.notification.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record NotificationUpdateRequest(
	@NotNull Boolean enabled,
	@NotNull List<@NotNull String> times
) {
}
