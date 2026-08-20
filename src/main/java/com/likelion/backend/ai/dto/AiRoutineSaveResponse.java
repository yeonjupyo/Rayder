package com.likelion.backend.ai.dto;

import com.likelion.backend.routine.dto.RoutineGroupResponse;

public record AiRoutineSaveResponse(RoutineGroupResponse morning, RoutineGroupResponse evening) {
}
