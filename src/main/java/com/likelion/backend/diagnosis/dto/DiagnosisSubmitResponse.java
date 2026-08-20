package com.likelion.backend.diagnosis.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class DiagnosisSubmitResponse {
    private int resultId;
    private String skinType; // "건성피부" 형태
    /** 결과 화면 상단 키워드. 예: ["푸석함", "건조함"] */
    private List<String> keywords;
    /** 결과 화면 설명 문구. DIAGNOSIS_RESULT.result_summary 에 저장한 값과 같다. */
    private String description;
}
