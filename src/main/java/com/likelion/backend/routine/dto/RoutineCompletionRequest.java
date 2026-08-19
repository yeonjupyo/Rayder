package com.likelion.backend.routine.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RoutineCompletionRequest(@NotNull LocalDate date, @NotNull Boolean completed) {
}
