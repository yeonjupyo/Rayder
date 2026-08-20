package com.likelion.backend.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.likelion.backend.ai.diagnosis.DiagnosisResult;
import com.likelion.backend.ai.diagnosis.DiagnosisResultMapper;
import com.likelion.backend.ai.dto.AiRoutineRecommendationRequest;
import com.likelion.backend.environment.dto.EnvironmentInfo;
import com.likelion.backend.environment.service.EnvironmentQueryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "ai.openai.api-key=${OPENAI_API_KEY:}")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class AiRoutineLiveIntegrationTest {
	@MockitoBean DiagnosisResultMapper diagnosisMapper;
	@MockitoBean EnvironmentQueryService environmentService;
	@Autowired AiRoutineRecommendationService service;

	@Test
	void retrievesPdfKnowledgeAndGeneratesValidatedRoutineWithRealOpenAi() {
		when(diagnosisMapper.findLatestByUserId(7L)).thenReturn(Optional.of(
			new DiagnosisResult(1L, 7L, "건성", "세안 후 당김과 볼 부위 건조함",
				LocalDateTime.of(2026, 8, 20, 9, 0))));
		when(environmentService.getUvByLocation(37.4979, 127.0276)).thenReturn(
			new EnvironmentInfo(EnvironmentInfo.Type.UV, 6.0, "높음", "서울특별시 강남구", LocalDateTime.now()));
		when(environmentService.getDustByLocation(37.4979, 127.0276)).thenReturn(List.of(
			new EnvironmentInfo(EnvironmentInfo.Type.DUST_PM10, 24.0, "좋음", "서울특별시 강남구", LocalDateTime.now()),
			new EnvironmentInfo(EnvironmentInfo.Type.DUST_PM25, 11.0, "좋음", "서울특별시 강남구", LocalDateTime.now())));

		var response = service.recommend(7L, new AiRoutineRecommendationRequest(37.4979, 127.0276));

		assertThat(response.skinType()).isEqualTo("건성");
		assertThat(response.diagnosisResult()).isEqualTo("세안 후 당김과 볼 부위 건조함");
		assertThat(response.environment().available()).isTrue();
		assertThat(response.morning()).hasSizeBetween(1, 5);
		assertThat(response.evening()).hasSizeBetween(1, 5);
		assertThat(response.reasons()).hasSizeLessThanOrEqualTo(3);
		assertThat(response.morning()).allSatisfy(item -> {
			assertThat(item.name()).isNotBlank().hasSizeLessThanOrEqualTo(20);
			assertThat(item.detail()).isNotBlank().hasSizeLessThanOrEqualTo(30);
		});
		assertThat(response.evening()).allSatisfy(item -> {
			assertThat(item.name()).isNotBlank().hasSizeLessThanOrEqualTo(20);
			assertThat(item.detail()).isNotBlank().hasSizeLessThanOrEqualTo(30);
		});
	}
}
