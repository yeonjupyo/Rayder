package com.likelion.backend.notification.dto;

import jakarta.validation.constraints.NotNull;

public record WarningSettingRequest(@NotNull Boolean enabled) {
}
