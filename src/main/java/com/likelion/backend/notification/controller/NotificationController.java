package com.likelion.backend.notification.controller;

import com.likelion.backend.notification.dto.NotificationListResponse;
import com.likelion.backend.notification.dto.NotificationSettingRequest;
import com.likelion.backend.notification.dto.NotificationSettingResponse;
import com.likelion.backend.notification.dto.NotificationUpdateRequest;
import com.likelion.backend.notification.dto.WarningSettingRequest;
import com.likelion.backend.notification.dto.WarningSettingResponse;
import com.likelion.backend.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
	private static final String AUTHENTICATED_USER_ID = "authenticatedUserId";
	private final NotificationService notificationService;

	@GetMapping
	public NotificationListResponse findAll(
		@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId
	) {
		return notificationService.findAll(userId);
	}

	@PostMapping
	public ResponseEntity<NotificationSettingResponse> create(
		@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@Valid @RequestBody NotificationSettingRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.create(userId, request));
	}

	@PutMapping("/{notificationId}")
	public NotificationSettingResponse update(
		@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@PathVariable long notificationId,
		@Valid @RequestBody NotificationUpdateRequest request
	) {
		return notificationService.update(notificationId, userId, request);
	}

	@DeleteMapping("/{notificationId}")
	public ResponseEntity<Void> delete(
		@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@PathVariable long notificationId
	) {
		notificationService.delete(notificationId, userId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/uv-exposure-warning")
	public WarningSettingResponse updateWarning(
		@RequestAttribute(name = AUTHENTICATED_USER_ID) long userId,
		@Valid @RequestBody WarningSettingRequest request
	) {
		return notificationService.updateWarning(userId, request.enabled());
	}
}
