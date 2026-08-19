package com.likelion.backend.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ai.rag")
public record RagProperties(String resource, int chunkSize, int chunkOverlap, int topK,
	double minimumSimilarity) {
	public RagProperties {
		resource = resource == null ? "classpath:rag/Rayder_RAG.pdf" : resource;
		chunkSize = chunkSize <= 0 ? 900 : chunkSize;
		chunkOverlap = chunkOverlap < 0 ? 150 : chunkOverlap;
		topK = topK <= 0 ? 4 : topK;
		minimumSimilarity = minimumSimilarity <= 0 ? 0.15 : minimumSimilarity;
	}
}
