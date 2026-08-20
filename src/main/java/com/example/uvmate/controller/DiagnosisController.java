package com.example.uvmate.controller;

import com.example.uvmate.dto.DiagnosisSubmitRequest;
import com.example.uvmate.dto.DiagnosisSubmitResponse;
import com.example.uvmate.service.DiagnosisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/diagnosis")
@RequiredArgsConstructor
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @PostMapping("/submit")
    public ResponseEntity<DiagnosisSubmitResponse> submit(@RequestBody DiagnosisSubmitRequest request) {
        return ResponseEntity.ok(diagnosisService.submit(request));
    }
}