package com.likelion.backend.ai.rag;

import java.util.List;

public interface KnowledgeRetriever {
	List<String> retrieve(String query);
}
