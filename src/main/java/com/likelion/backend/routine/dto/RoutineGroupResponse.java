package com.likelion.backend.routine.dto;

import com.likelion.backend.routine.domain.RoutineType;
import java.util.List;

public record RoutineGroupResponse(Long routineId, RoutineType type, List<RoutineItemResponse> items) {
}
