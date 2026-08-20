package com.likelion.backend.ai.rag;

import java.util.List;

public interface EmbeddingClient {
	List<double[]> embed(List<String> input);
}
