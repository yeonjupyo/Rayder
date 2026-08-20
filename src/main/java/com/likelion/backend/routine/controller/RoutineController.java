package com.likelion.backend.routine.controller;

import com.likelion.backend.ai.dto.AiRoutineSaveRequest;
import com.likelion.backend.ai.dto.AiRoutineSaveResponse;
import com.likelion.backend.routine.dto.MyRoutineResponse;
import com.likelion.backend.routine.dto.RoutineCompletionRequest;
import com.likelion.backend.routine.dto.RoutineCreateRequest;
import com.likelion.backend.routine.dto.RoutineGroupResponse;
import com.likelion.backend.routine.dto.RoutineItemCreateRequest;
import com.likelion.backend.routine.dto.RoutineItemResponse;
import com.likelion.backend.routine.dto.RoutineItemUpdateRequest;
import com.likelion.backend.routine.dto.RoutineOrderRequest;
import com.likelion.backend.routine.service.RoutineService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoutineController {
	private static final String AUTHENTICATED_USER_ID = "authenticatedUserId";
	private final RoutineService service;

	@GetMapping("/routines")
	public MyRoutineResponse findAll(@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@RequestParam String date) {
		return service.findAll(userId, service.parseDate(date));
	}

	@PostMapping("/routines")
	public ResponseEntity<RoutineGroupResponse> createRoutine(
		@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@Valid @RequestBody RoutineCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.createRoutine(userId, request.type()));
	}

	@PostMapping("/routines/from-ai")
	public ResponseEntity<AiRoutineSaveResponse> saveFromAi(
		@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@Valid @RequestBody AiRoutineSaveRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.saveFromAi(userId, request));
	}

	@PostMapping("/routines/{routineId}/items")
	public ResponseEntity<RoutineItemResponse> addItem(
		@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@PathVariable long routineId, @Valid @RequestBody RoutineItemCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.addItem(routineId, userId, request));
	}

	@PatchMapping("/routine-items/{itemId}")
	public RoutineItemResponse updateItem(@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@PathVariable long itemId, @Valid @RequestBody RoutineItemUpdateRequest request) {
		return service.updateItem(itemId, userId, request);
	}

	@DeleteMapping("/routine-items/{itemId}")
	public ResponseEntity<Void> deleteItem(@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@PathVariable long itemId) {
		service.deleteItem(itemId, userId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/routines/{routineId}/items/order")
	public List<RoutineItemResponse> reorder(@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@PathVariable long routineId, @Valid @RequestBody RoutineOrderRequest request) {
		return service.reorder(routineId, userId, request);
	}

	@PutMapping("/routine-items/{itemId}/completion")
	public RoutineItemResponse updateCompletion(@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@PathVariable long itemId, @Valid @RequestBody RoutineCompletionRequest request) {
		return service.updateCompletion(itemId, userId, request);
	}
}
