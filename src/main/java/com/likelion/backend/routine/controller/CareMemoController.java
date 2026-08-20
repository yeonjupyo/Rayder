package com.likelion.backend.routine.controller;

import com.likelion.backend.routine.dto.CareMemoCompletionRequest;
import com.likelion.backend.routine.dto.CareMemoCreateRequest;
import com.likelion.backend.routine.dto.CareMemoResponse;
import com.likelion.backend.routine.dto.CareMemoUpdateRequest;
import com.likelion.backend.routine.service.RoutineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/care-memos")
@RequiredArgsConstructor
public class CareMemoController {
	private static final String AUTHENTICATED_USER_ID = "authenticatedUserId";
	private final RoutineService service;

	@PostMapping
	public ResponseEntity<CareMemoResponse> create(@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@Valid @RequestBody CareMemoCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.addMemo(userId, request));
	}

	@PatchMapping("/{memoId}")
	public CareMemoResponse update(@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@PathVariable long memoId, @Valid @RequestBody CareMemoUpdateRequest request) {
		return service.updateMemo(memoId, userId, request.content());
	}

	@PutMapping("/{memoId}/completion")
	public CareMemoResponse updateCompletion(@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@PathVariable long memoId, @Valid @RequestBody CareMemoCompletionRequest request) {
		return service.updateMemoCompletion(memoId, userId, request.completed());
	}

	@DeleteMapping("/{memoId}")
	public ResponseEntity<Void> delete(@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@PathVariable long memoId) {
		service.deleteMemo(memoId, userId);
		return ResponseEntity.noContent().build();
	}
}
