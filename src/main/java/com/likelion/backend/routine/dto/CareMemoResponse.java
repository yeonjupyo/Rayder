package com.likelion.backend.routine.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CareMemoResponse(
	Long id, LocalDate date, String content, boolean done,
	LocalDateTime createdAt, LocalDateTime updatedAt
) {
}
