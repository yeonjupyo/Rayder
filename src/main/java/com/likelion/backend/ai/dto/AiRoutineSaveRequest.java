package com.likelion.backend.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiRoutineSaveRequest(
	@NotNull @Size(max = 5) List<@Valid AiRoutineItem> morning,
	@NotNull @Size(max = 5) List<@Valid AiRoutineItem> evening) {
}
