package com.likelion.backend.skinmon.controller;

import com.likelion.backend.skinmon.dto.SkinmonCreateRequest;
import com.likelion.backend.skinmon.dto.SkinmonCreateResponse;
import com.likelion.backend.skinmon.service.SkinmonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/skinmon")
@RequiredArgsConstructor
public class SkinmonController {

    private final SkinmonService skinmonService;

    @PostMapping
    public ResponseEntity<SkinmonCreateResponse> create(@Valid @RequestBody SkinmonCreateRequest request) {
        return ResponseEntity.ok(skinmonService.create(request));
    }
}
