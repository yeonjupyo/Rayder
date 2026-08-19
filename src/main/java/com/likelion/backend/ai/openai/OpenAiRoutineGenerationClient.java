package com.likelion.backend.ai.openai;

import com.likelion.backend.ai.config.OpenAiProperties;
import com.likelion.backend.common.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Component
public class OpenAiRoutineGenerationClient implements RoutineGenerationClient {
	private final RestClient client;
	private final OpenAiProperties properties;
	private final OpenAiHttpSupport http;
	private final ObjectMapper objectMapper;
	public OpenAiRoutineGenerationClient(@Qualifier("openAiRestClient") RestClient client,
		OpenAiProperties properties, OpenAiHttpSupport http, ObjectMapper objectMapper) {
		this.client = client; this.properties = properties; this.http = http; this.objectMapper = objectMapper;
	}

	@Override public GeneratedRoutine generate(String prompt) {
		Map<String, Object> request = Map.of(
			"model", properties.chatModel(), "store", false,
			"instructions", "You are a cautious Korean skincare routine assistant. Follow supplied evidence and schema exactly.",
			"input", prompt, "text", Map.of("format", schema()));
		ResponsesResponse response = http.execute(() -> client.post().uri("/v1/responses").body(request)
			.retrieve().body(ResponsesResponse.class), "OPENAI_API_ERROR");
		String text = outputText(response);
		try { return objectMapper.readValue(text, GeneratedRoutine.class); }
		catch (Exception ex) { throw new BusinessException("OPENAI_INVALID_RESPONSE",
			"OpenAI returned an invalid structured response", HttpStatus.BAD_GATEWAY); }
	}

	private String outputText(ResponsesResponse response) {
		if (response != null && response.output() != null)
			for (Output output : response.output()) if (output.content() != null)
				for (Content content : output.content()) if ("output_text".equals(content.type()) && content.text() != null) return content.text();
		throw new BusinessException("OPENAI_INVALID_RESPONSE", "OpenAI response has no output text", HttpStatus.BAD_GATEWAY);
	}

	private Map<String, Object> schema() {
		Map<String, Object> item = Map.of("type", "object", "additionalProperties", false,
			"required", List.of("order", "name", "detail"), "properties", Map.of(
				"order", Map.of("type", "integer", "minimum", 1, "maximum", 5),
				"name", Map.of("type", "string", "minLength", 1, "maxLength", 20),
				"detail", Map.of("type", "string", "minLength", 1, "maxLength", 30)));
		Map<String, Object> root = Map.of("type", "object", "additionalProperties", false,
			"required", List.of("morning", "evening", "reasons"), "properties", Map.of(
				"morning", Map.of("type", "array", "minItems", 1, "maxItems", 5, "items", item),
				"evening", Map.of("type", "array", "minItems", 1, "maxItems", 5, "items", item),
				"reasons", Map.of("type", "array", "maxItems", 3, "items", Map.of("type", "string"))));
		return Map.of("type", "json_schema", "name", "ai_skincare_routine", "strict", true, "schema", root);
	}
	private record ResponsesResponse(List<Output> output) {}
	private record Output(List<Content> content) {}
	private record Content(String type, String text) {}
}
