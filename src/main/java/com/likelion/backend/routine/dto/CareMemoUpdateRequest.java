package com.likelion.backend.routine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CareMemoUpdateRequest(@NotBlank @Size(max = 255) String content) {
}
