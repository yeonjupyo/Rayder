package com.likelion.backend.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiRoutineItem(
	@Min(1) @Max(5) int order,
	@NotBlank @Size(max = 20) String name,
	@NotBlank @Size(max = 30) String detail) {
}
