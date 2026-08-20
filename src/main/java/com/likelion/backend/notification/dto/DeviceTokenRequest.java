package com.likelion.backend.notification.dto;

import com.likelion.backend.notification.domain.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record DeviceTokenRequest(@NotBlank @Size(max = 512)
	@Pattern(regexp = "^(Expo(nent)?PushToken)\\[[^]]+\\]$", message = "must be an Expo push token") String token,
	@NotNull DevicePlatform platform) { }
