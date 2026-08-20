package com.likelion.backend.skinmon.controller;

import com.likelion.backend.skinmon.dto.SkinmonCreateRequest;
import com.likelion.backend.skinmon.dto.SkinmonCreateResponse;
import com.likelion.backend.skinmon.dto.SkinmonExpressionUpdateRequest;
import com.likelion.backend.skinmon.service.SkinmonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skinmon")
@RequiredArgsConstructor
public class SkinmonController {

    private final SkinmonService skinmonService;

    @PostMapping
    public ResponseEntity<SkinmonCreateResponse> create(@RequestBody SkinmonCreateRequest request) {
        return ResponseEntity.ok(skinmonService.create(request));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<SkinmonCreateResponse> get(@PathVariable int userId) {
        return ResponseEntity.ok(skinmonService.getByUserId(userId));
    }

    @PatchMapping("/{skinmonId}/expression")
    public ResponseEntity<SkinmonCreateResponse> updateExpression(@PathVariable int skinmonId,
                                                                  @RequestBody SkinmonExpressionUpdateRequest request) {
        return ResponseEntity.ok(skinmonService.updateExpression(skinmonId, request.getExpressionType()));
    }
}