package com.likelion.backend.diagnosis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DiagnosisSubmitResponse {
    private int resultId;
    private String skinType; // "건성피부" 형태
}
