package com.likelion.backend.routine.dto;

import com.likelion.backend.routine.domain.RoutineType;
import jakarta.validation.constraints.NotNull;

public record RoutineCreateRequest(@NotNull RoutineType type) {
}
