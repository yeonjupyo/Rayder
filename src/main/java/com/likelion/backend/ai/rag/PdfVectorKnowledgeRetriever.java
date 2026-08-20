package com.likelion.backend.ai.rag;

import com.likelion.backend.ai.config.RagProperties;
import com.likelion.backend.common.exception.BusinessException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PdfVectorKnowledgeRetriever implements KnowledgeRetriever {
	private final EmbeddingClient embeddingClient;
	private final RagProperties properties;
	private final List<String> chunks;
	private volatile List<double[]> vectors;

	public PdfVectorKnowledgeRetriever(EmbeddingClient embeddingClient, RagProperties properties,
		ResourceLoader resourceLoader) {
		this.embeddingClient = embeddingClient; this.properties = properties;
		this.chunks = loadChunks(resourceLoader);
	}

	@Override public List<String> retrieve(String query) {
		List<double[]> localVectors = vectors;
		if (localVectors == null) {
			synchronized (this) {
				if (vectors == null) vectors = List.copyOf(embeddingClient.embed(chunks));
				localVectors = vectors;
			}
		}
		double[] queryVector = embeddingClient.embed(List.of(query)).get(0);
		List<ScoredChunk> scored = new ArrayList<>();
		for (int i = 0; i < chunks.size(); i++) {
			double score = cosine(queryVector, localVectors.get(i));
			if (score >= properties.minimumSimilarity()) scored.add(new ScoredChunk(chunks.get(i), score));
		}
		return scored.stream().sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
			.limit(properties.topK()).map(ScoredChunk::text).toList();
	}

	private List<String> loadChunks(ResourceLoader loader) {
		try (var input = loader.getResource(properties.resource()).getInputStream();
			 var document = Loader.loadPDF(input.readAllBytes())) {
			String text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ").trim();
			if (text.isBlank()) throw new IOException("PDF contains no extractable text");
			int size = Math.max(200, properties.chunkSize());
			int overlap = Math.max(0, Math.min(properties.chunkOverlap(), size - 1));
			List<String> result = new ArrayList<>();
			for (int start = 0; start < text.length(); start += size - overlap) {
				int end = Math.min(text.length(), start + size);
				result.add(text.substring(start, end));
				if (end == text.length()) break;
			}
			return List.copyOf(result);
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to load RAG knowledge base: " + properties.resource(), ex);
		}
	}

	private double cosine(double[] left, double[] right) {
		if (left.length != right.length) throw new BusinessException("RAG_RETRIEVAL_ERROR",
			"Embedding dimensions do not match", HttpStatus.BAD_GATEWAY);
		double dot = 0, leftNorm = 0, rightNorm = 0;
		for (int i = 0; i < left.length; i++) { dot += left[i] * right[i]; leftNorm += left[i] * left[i]; rightNorm += right[i] * right[i]; }
		return leftNorm == 0 || rightNorm == 0 ? 0 : dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
	}
	private record ScoredChunk(String text, double score) {}
}
