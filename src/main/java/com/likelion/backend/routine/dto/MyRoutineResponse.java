package com.likelion.backend.routine.dto;

import java.time.LocalDate;
import java.util.List;

public record MyRoutineResponse(
	LocalDate date,
	RoutineGroupResponse morning,
	RoutineGroupResponse evening,
	List<CareMemoResponse> memos,
	int completedCount,
	int totalCount,
	int progressRate
) {
}
