package com.likelion.backend.ai.diagnosis;

import java.time.LocalDateTime;

public record DiagnosisResult(long resultId, long userId, String skinType, String diagnosisResult,
	LocalDateTime diagnosedAt) {
}
