package com.likelion.backend.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceTokenDeleteRequest(@NotBlank @Size(max = 512) String token) { }
