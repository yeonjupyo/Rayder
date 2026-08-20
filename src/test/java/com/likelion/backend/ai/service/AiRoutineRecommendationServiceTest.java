package com.likelion.backend.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.likelion.backend.ai.diagnosis.DiagnosisResult;
import com.likelion.backend.ai.diagnosis.DiagnosisResultMapper;
import com.likelion.backend.ai.dto.AiRoutineItem;
import com.likelion.backend.ai.dto.AiRoutineRecommendationRequest;
import com.likelion.backend.ai.openai.RoutineGenerationClient;
import com.likelion.backend.ai.rag.KnowledgeRetriever;
import com.likelion.backend.common.exception.BusinessException;
import com.likelion.backend.environment.dto.EnvironmentInfo;
import com.likelion.backend.environment.service.EnvironmentQueryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiRoutineRecommendationServiceTest {
	@Mock DiagnosisResultMapper diagnosisMapper;
	@Mock EnvironmentQueryService environmentService;
	@Mock KnowledgeRetriever retriever;
	@Mock RoutineGenerationClient generationClient;
	AiRoutineRecommendationService service;

	@BeforeEach void setUp() {
		service = new AiRoutineRecommendationService(diagnosisMapper, environmentService, retriever, generationClient);
	}

	@Test void generatesFromLatestDiagnosisEnvironmentAndRag() {
		stubDiagnosis();
		when(environmentService.getUvByLocation(37.5, 127.0)).thenReturn(info(EnvironmentInfo.Type.UV, 5, "보통"));
		when(environmentService.getDustByLocation(37.5, 127.0)).thenReturn(List.of(
			info(EnvironmentInfo.Type.DUST_PM10, 20, "좋음"), info(EnvironmentInfo.Type.DUST_PM25, 8, "좋음")));
		when(retriever.retrieve(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of("근거 문서"));
		when(generationClient.generate(org.mockito.ArgumentMatchers.contains("근거 문서"))).thenReturn(valid());

		var result = service.recommend(7L, new AiRoutineRecommendationRequest(37.5, 127.0));

		assertThat(result.skinType()).isEqualTo("건성");
		assertThat(result.environment().available()).isTrue();
		assertThat(result.morning()).hasSize(1);
	}

	@Test void environmentFailureFallsBackWithoutInventedValues() {
		stubDiagnosis();
		when(environmentService.getUvByLocation(37.5, 127.0)).thenThrow(new RuntimeException("provider down"));
		when(retriever.retrieve(org.mockito.ArgumentMatchers.contains("Unavailable"))).thenReturn(List.of("근거"));
		when(generationClient.generate(org.mockito.ArgumentMatchers.contains("Unavailable"))).thenReturn(valid());

		var result = service.recommend(7L, new AiRoutineRecommendationRequest(37.5, 127.0));

		assertThat(result.environment().available()).isFalse();
		assertThat(result.environment().uvLevel()).isNull();
	}

	@Test void missingDiagnosisIsNotFound() {
		when(diagnosisMapper.findLatestByUserId(7L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.recommend(7L, new AiRoutineRecommendationRequest(37.5, 127.0)))
			.isInstanceOf(BusinessException.class).extracting("code").isEqualTo("DIAGNOSIS_RESULT_NOT_FOUND");
	}

	@Test void emptyRetrievalIsRejected() {
		stubDiagnosis();
		when(environmentService.getUvByLocation(37.5, 127.0)).thenThrow(new RuntimeException());
		when(retriever.retrieve(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
		assertThatThrownBy(() -> service.recommend(7L, new AiRoutineRecommendationRequest(37.5, 127.0)))
			.isInstanceOf(BusinessException.class).extracting("code").isEqualTo("RAG_CONTEXT_NOT_FOUND");
	}

	@Test void nonConsecutiveModelOrderIsRejected() {
		stubDiagnosis();
		when(environmentService.getUvByLocation(37.5, 127.0)).thenThrow(new RuntimeException());
		when(retriever.retrieve(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of("근거"));
		when(generationClient.generate(org.mockito.ArgumentMatchers.anyString())).thenReturn(
			new RoutineGenerationClient.GeneratedRoutine(List.of(new AiRoutineItem(2, "세안", "순하게 세안")), valid().evening(), List.of()));
		assertThatThrownBy(() -> service.recommend(7L, new AiRoutineRecommendationRequest(37.5, 127.0)))
			.isInstanceOf(BusinessException.class).extracting("code").isEqualTo("OPENAI_INVALID_RESPONSE");
	}

	private void stubDiagnosis() { when(diagnosisMapper.findLatestByUserId(7L)).thenReturn(Optional.of(
		new DiagnosisResult(3, 7, "건성", "수분 부족", LocalDateTime.now()))); }
	private EnvironmentInfo info(EnvironmentInfo.Type type, double value, String level) {
		return new EnvironmentInfo(type, value, level, "서울", LocalDateTime.now());
	}
	private RoutineGenerationClient.GeneratedRoutine valid() { return new RoutineGenerationClient.GeneratedRoutine(
		List.of(new AiRoutineItem(1, "약산성 세안", "미온수로 부드럽게 세안")),
		List.of(new AiRoutineItem(1, "저자극 세안", "잔여물을 부드럽게 제거")), List.of("장벽 보호 중심 구성")); }
}
