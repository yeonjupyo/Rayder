package com.likelion.backend.diagnosis.constant;

import java.util.List;

public class DiagnosisConstants {
    public static final List<String> QUESTIONS = List.of(
            "평소 피부에 유분이 많아 번들거리나요?",
            "화장품을 바꾸면 피부가 쉽게 예민해지나요?",
            "자외선에 노출되면 피부가 쉽게 붉어지나요?",
            "평소 세안 후 피부 당김이 느껴지나요?",
            "T존(이마·코)과 볼의 피부 상태 차이가 뚜렷한가요?",
            "실내에 있어도 피부가 쉽게 건조해지나요?",
            "미세먼지가 심한 날 피부가 답답하게 느껴지나요?"
    );

    public static int scoreOf(String answer) {
        return switch (answer) {
            case "stronglyAgree" -> 3;
            case "slightlyAgree" -> 2;
            case "neutral" -> 1;
            case "disagree" -> 0;
            default -> throw new IllegalArgumentException("알 수 없는 답변: " + answer);
        };
    }
}
