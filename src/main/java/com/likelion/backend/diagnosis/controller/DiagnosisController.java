package com.likelion.backend.diagnosis.controller;

import com.likelion.backend.diagnosis.dto.DiagnosisSubmitRequest;
import com.likelion.backend.diagnosis.dto.DiagnosisSubmitResponse;
import com.likelion.backend.diagnosis.service.DiagnosisService;
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
