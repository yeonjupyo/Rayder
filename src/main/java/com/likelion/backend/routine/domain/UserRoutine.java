package com.likelion.backend.routine.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserRoutine {
	private Long routineId;
	private Long userId;
	private RoutineType type;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
