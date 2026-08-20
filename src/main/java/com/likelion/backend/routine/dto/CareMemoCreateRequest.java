package com.likelion.backend.routine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CareMemoCreateRequest(
	@NotNull LocalDate date,
	@NotBlank @Size(max = 255) String content
) {
}
