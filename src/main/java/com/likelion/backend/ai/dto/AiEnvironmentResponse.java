package com.likelion.backend.ai.dto;

public record AiEnvironmentResponse(boolean available, String uvLevel, String dustLevel) {
	public static AiEnvironmentResponse unavailable() { return new AiEnvironmentResponse(false, null, null); }
}
