package com.likelion.backend.notification.dto;

import java.time.LocalDateTime;

public record WarningSettingResponse(
	boolean enabled,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
