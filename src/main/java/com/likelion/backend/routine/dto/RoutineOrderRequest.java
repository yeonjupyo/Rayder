package com.likelion.backend.routine.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RoutineOrderRequest(@NotEmpty List<@NotNull Long> itemIds) {
}
