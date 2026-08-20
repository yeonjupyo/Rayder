package com.likelion.backend.ai.controller;

import com.likelion.backend.ai.dto.AiRoutineRecommendationRequest;
import com.likelion.backend.ai.dto.AiRoutineRecommendationResponse;
import com.likelion.backend.ai.service.AiRoutineRecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-routines")
@RequiredArgsConstructor
public class AiRoutineController {
	private static final String AUTHENTICATED_USER_ID = "authenticatedUserId";
	private final AiRoutineRecommendationService service;
	@PostMapping("/recommend")
	public AiRoutineRecommendationResponse recommend(
		@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@Valid @RequestBody AiRoutineRecommendationRequest request) {
		return service.recommend(userId, request);
	}
}
