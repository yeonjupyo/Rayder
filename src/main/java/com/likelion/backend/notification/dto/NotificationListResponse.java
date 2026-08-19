package com.likelion.backend.notification.dto;

import java.util.List;

public record NotificationListResponse(
	List<NotificationSettingResponse> notifications,
	WarningSettingResponse uvRiskWarning
) {
}
