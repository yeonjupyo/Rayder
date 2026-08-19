package com.likelion.backend.routine.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CareMemo {
	private Long memoId;
	private Long userId;
	private LocalDate targetDate;
	private String content;
	private boolean completed;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
