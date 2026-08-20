package com.likelion.backend.diagnosis.dto;

import lombok.Data;
import java.util.List;

@Data
public class DiagnosisSubmitRequest {
    private int userId;
    private List<String> answers; // 7개, QUESTIONS 순서와 동일
}
