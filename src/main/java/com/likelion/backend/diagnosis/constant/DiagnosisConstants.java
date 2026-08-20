package com.likelion.backend.diagnosis.constant;

import java.util.List;
import java.util.Map;

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

    /**
     * 피부타입별 결과 화면 카피. 진단 결과 화면의 키워드·설명이 프론트 더미 상수에 박혀 있었는데,
     * 판정 로직이 서버에 있으므로 카피도 서버에서 함께 내려 한 곳에서 관리한다.
     * 설명 문구는 DIAGNOSIS_RESULT.result_summary 에 그대로 저장되고 AI 추천의 입력으로도 쓰인다.
     */
    private static final Map<String, Copy> COPY = Map.of(
            "건성", new Copy(List.of("푸석함", "건조함"),
                    "피부의 유분과 수분이 부족해 세안 후 당김이나 건조함을 쉽게 느낄 수 있어요."),
            "지성", new Copy(List.of("번들거림", "모공"),
                    "유분이 많아 번들거리기 쉽고, 모공과 피지 관리가 필요해요."),
            "복합성", new Copy(List.of("T존 유분", "볼 건조"),
                    "T존은 번들거리고 볼은 건조한 편이라 부위별로 다르게 관리해야 해요."),
            "민감성", new Copy(List.of("붉어짐", "예민함"),
                    "외부 자극에 쉽게 붉어지고 예민해지기 때문에 진정과 장벽 케어가 우선이에요.")
    );

    private static final Copy FALLBACK_COPY = new Copy(List.of("수분", "장벽"),
            "피부 상태에 맞춰 수분과 장벽 관리를 중심으로 케어해 주세요.");

    /** 판정된 피부타입("건성", "지성", "복합성", "민감성")의 카피를 돌려준다. */
    public static Copy copyOf(String skinType) {
        return COPY.getOrDefault(skinType, FALLBACK_COPY);
    }

    /** 결과 화면에 노출하는 키워드와 설명. */
    public record Copy(List<String> keywords, String description) {
    }
}

