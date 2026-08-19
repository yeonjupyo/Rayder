package com.likelion.backend.ai.openai;

import com.likelion.backend.ai.dto.AiRoutineItem;
import java.util.List;

public interface RoutineGenerationClient {
	GeneratedRoutine generate(String prompt);
	record GeneratedRoutine(List<AiRoutineItem> morning, List<AiRoutineItem> evening, List<String> reasons) {}
}
