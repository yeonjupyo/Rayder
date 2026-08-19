package com.likelion.backend.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationLocationRequest(
	@NotBlank @Size(max = 30) String sido,
	@NotBlank @Size(max = 30) String gugun) { }
