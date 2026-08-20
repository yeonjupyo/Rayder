package com.example.uvmate.controller;

import com.example.uvmate.dto.SkinmonCreateRequest;
import com.example.uvmate.dto.SkinmonCreateResponse;
import com.example.uvmate.service.SkinmonService;
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
}