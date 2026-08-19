package com.likelion.backend.ai.dto;

import java.util.List;

public record AiRoutineRecommendationResponse(String skinType, String diagnosisResult,
	AiEnvironmentResponse environment, List<AiRoutineItem> morning, List<AiRoutineItem> evening,
	List<String> reasons) {
}
