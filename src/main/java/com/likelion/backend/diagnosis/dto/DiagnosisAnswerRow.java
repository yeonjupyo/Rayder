package com.likelion.backend.diagnosis.dto;

import lombok.Data;

@Data
public class DiagnosisAnswerRow {
    private int userId;
    private int questionNo;
    private String questionContent;
    private String answerValue;
}
