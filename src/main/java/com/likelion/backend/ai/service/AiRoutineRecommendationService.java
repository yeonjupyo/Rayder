package com.likelion.backend.ai.service;

import com.likelion.backend.ai.diagnosis.DiagnosisResult;
import com.likelion.backend.ai.diagnosis.DiagnosisResultMapper;
import com.likelion.backend.ai.dto.AiEnvironmentResponse;
import com.likelion.backend.ai.dto.AiRoutineItem;
import com.likelion.backend.ai.dto.AiRoutineRecommendationRequest;
import com.likelion.backend.ai.dto.AiRoutineRecommendationResponse;
import com.likelion.backend.ai.openai.RoutineGenerationClient;
import com.likelion.backend.ai.rag.KnowledgeRetriever;
import com.likelion.backend.common.exception.BusinessException;
import com.likelion.backend.environment.dto.EnvironmentInfo;
import com.likelion.backend.environment.service.EnvironmentQueryService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AiRoutineRecommendationService {
	private final DiagnosisResultMapper diagnosisMapper;
	private final EnvironmentQueryService environmentService;
	private final KnowledgeRetriever retriever;
	private final RoutineGenerationClient generationClient;
	public AiRoutineRecommendationService(DiagnosisResultMapper diagnosisMapper,
		EnvironmentQueryService environmentService, KnowledgeRetriever retriever,
		RoutineGenerationClient generationClient) {
		this.diagnosisMapper = diagnosisMapper; this.environmentService = environmentService;
		this.retriever = retriever; this.generationClient = generationClient;
	}

	public AiRoutineRecommendationResponse recommend(long userId, AiRoutineRecommendationRequest request) {
		DiagnosisResult diagnosis = diagnosisMapper.findLatestByUserId(userId).orElseThrow(() ->
			new BusinessException("DIAGNOSIS_RESULT_NOT_FOUND", "No diagnosis result found", HttpStatus.NOT_FOUND));
		EnvironmentContext environment = environment(request.latitude(), request.longitude());
		String query = retrievalQuery(diagnosis, environment);
		List<String> context;
		try { context = retriever.retrieve(query); }
		catch (BusinessException ex) { throw ex; }
		catch (RuntimeException ex) { throw new BusinessException("RAG_RETRIEVAL_ERROR", "RAG retrieval failed", HttpStatus.BAD_GATEWAY); }
		if (context.isEmpty()) throw new BusinessException("RAG_CONTEXT_NOT_FOUND", "No relevant knowledge was found", HttpStatus.BAD_GATEWAY);
		var generated = generationClient.generate(prompt(diagnosis, environment, context));
		validate(generated);
		return new AiRoutineRecommendationResponse(diagnosis.skinType(), diagnosis.diagnosisResult(),
			environment.response(), generated.morning(), generated.evening(), generated.reasons());
	}

	private EnvironmentContext environment(double latitude, double longitude) {
		try {
			EnvironmentInfo uv = environmentService.getUvByLocation(latitude, longitude);
			List<EnvironmentInfo> dust = environmentService.getDustByLocation(latitude, longitude);
			EnvironmentInfo pm10 = dust.stream().filter(v -> v.type() == EnvironmentInfo.Type.DUST_PM10).findFirst().orElseThrow();
			EnvironmentInfo pm25 = dust.stream().filter(v -> v.type() == EnvironmentInfo.Type.DUST_PM25).findFirst().orElseThrow();
			String dustLevel = pm10.level() + " / " + pm25.level();
			return new EnvironmentContext(uv, pm10, pm25, new AiEnvironmentResponse(true, uv.level(), dustLevel));
		} catch (RuntimeException ex) { return new EnvironmentContext(null, null, null, AiEnvironmentResponse.unavailable()); }
	}

	private String retrievalQuery(DiagnosisResult d, EnvironmentContext e) {
		return "피부 타입 " + d.skinType() + ", 진단 " + nullSafe(d.diagnosisResult()) + ", 환경 " + e.description();
	}
	private String prompt(DiagnosisResult d, EnvironmentContext e, List<String> context) {
		return """
			[USER SKIN]
			skinType: %s
			diagnosisResult: %s
			[ENVIRONMENT]
			%s
			[RAG CONTEXT]
			%s
			[OUTPUT SCHEMA]
			morning[1..5], evening[1..5], reasons[0..3]; item={order,name,detail}
			[OUTPUT RULES]
			한국어로 작성하세요. 각 배열 order는 1부터 연속이어야 합니다. name은 20자 이하, detail은 30자 이하의 짧은 체크리스트 설명입니다.
			검색 근거 밖의 의학 사실을 확대하지 말고 질병 진단, 처방, 약 추천, 치료 효과 보장을 하지 마세요. 일반적인 비의료 스킨케어만 추천하세요.
			환경 정보가 Unavailable이면 환경 수치나 등급을 추측하지 마세요.
			""".formatted(d.skinType(), nullSafe(d.diagnosisResult()), e.description(), String.join("\n---\n", context));
	}
	private void validate(RoutineGenerationClient.GeneratedRoutine value) {
		if (value == null || value.morning() == null || value.evening() == null || value.reasons() == null
			|| value.morning().isEmpty() || value.evening().isEmpty() || value.morning().size() > 5
			|| value.evening().size() > 5 || value.reasons().size() > 3
			|| !validItems(value.morning()) || !validItems(value.evening()))
			throw new BusinessException("OPENAI_INVALID_RESPONSE", "OpenAI response violates routine constraints", HttpStatus.BAD_GATEWAY);
	}
	private boolean validItems(List<AiRoutineItem> items) {
		for (int i = 0; i < items.size(); i++) { AiRoutineItem item = items.get(i);
			if (item == null || item.order() != i + 1 || item.name() == null || item.name().isBlank()
				|| item.name().length() > 20 || item.detail() == null || item.detail().isBlank() || item.detail().length() > 30) return false; }
		return true;
	}
	private String nullSafe(String value) { return value == null ? "없음" : value; }
	private record EnvironmentContext(EnvironmentInfo uv, EnvironmentInfo pm10, EnvironmentInfo pm25,
		AiEnvironmentResponse response) {
		String description() { return response.available() ? "UV: " + uv.value() + " (" + uv.level() + ")\nPM10: " + pm10.value() + " (" + pm10.level() + ")\nPM2.5: " + pm25.value() + " (" + pm25.level() + ")" : "Unavailable"; }
	}
}
