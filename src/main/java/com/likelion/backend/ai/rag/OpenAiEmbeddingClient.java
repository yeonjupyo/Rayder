package com.likelion.backend.ai.rag;

import com.likelion.backend.ai.config.OpenAiProperties;
import com.likelion.backend.ai.openai.OpenAiHttpSupport;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiEmbeddingClient implements EmbeddingClient {
	private final RestClient client;
	private final OpenAiProperties properties;
	private final OpenAiHttpSupport http;
	public OpenAiEmbeddingClient(@Qualifier("openAiRestClient") RestClient client,
		OpenAiProperties properties, OpenAiHttpSupport http) {
		this.client = client; this.properties = properties; this.http = http;
	}
	@Override public List<double[]> embed(List<String> input) {
		if (input.isEmpty()) return List.of();
		EmbeddingResponse response = http.execute(() -> client.post().uri("/v1/embeddings")
			.body(new EmbeddingRequest(properties.embeddingModel(), input, "float"))
			.retrieve().body(EmbeddingResponse.class), "OPENAI_EMBEDDING_ERROR");
		if (response == null || response.data() == null || response.data().size() != input.size())
			throw new com.likelion.backend.common.exception.BusinessException("OPENAI_EMBEDDING_ERROR",
				"Invalid embedding response", org.springframework.http.HttpStatus.BAD_GATEWAY);
		return response.data().stream().sorted(Comparator.comparingInt(EmbeddingData::index))
			.map(data -> data.embedding().stream().mapToDouble(Double::doubleValue).toArray()).toList();
	}
	private record EmbeddingRequest(String model, List<String> input, String encoding_format) {}
	private record EmbeddingResponse(List<EmbeddingData> data) {}
	private record EmbeddingData(int index, List<Double> embedding) {}
}
