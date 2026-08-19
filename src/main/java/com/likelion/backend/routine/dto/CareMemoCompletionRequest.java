package com.likelion.backend.routine.dto;

import jakarta.validation.constraints.NotNull;

public record CareMemoCompletionRequest(@NotNull Boolean completed) {
}
