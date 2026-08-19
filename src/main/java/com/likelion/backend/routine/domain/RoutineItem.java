package com.likelion.backend.routine.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoutineItem {
	private Long itemId;
	private Long routineId;
	private Long userId;
	private String name;
	private String detail;
	private Integer stepOrder;
	private boolean aiRecommended;
	private boolean completed;
	private LocalDateTime deletedAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
