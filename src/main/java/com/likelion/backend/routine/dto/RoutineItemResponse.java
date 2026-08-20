package com.likelion.backend.routine.dto;

public record RoutineItemResponse(Long id, String name, String detail, boolean done, int order) {
}
