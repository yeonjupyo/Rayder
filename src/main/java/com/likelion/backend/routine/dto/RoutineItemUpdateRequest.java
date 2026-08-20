package com.likelion.backend.routine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoutineItemUpdateRequest(
	@NotBlank @Size(max = 50) String name,
	@Size(max = 100) String detail
) {
}
