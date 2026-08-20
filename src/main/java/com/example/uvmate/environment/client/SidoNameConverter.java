package com.example.uvmate.environment.client;

import java.util.Map;

/**
 * 카카오 API가 돌려주는 "서울특별시" 같은 정식 명칭을,
 * 에어코리아 API가 요구하는 "서울" 같은 축약 명칭으로 변환.
 * 기상청 지역코드(kma-area-codes.csv)는 정식 명칭 그대로 써도 되지만
 * (RegionResolver가 contains()로 매칭), 에어코리아 sidoName 파라미터는
 * 반드시 이 축약 형태여야 함 (문서 "시도 이름" 항목 참고).
 */
public final class SidoNameConverter {

    private static final Map<String, String> FULL_TO_SHORT = Map.ofEntries(
            Map.entry("서울특별시", "서울"),
            Map.entry("부산광역시", "부산"),
            Map.entry("대구광역시", "대구"),
            Map.entry("인천광역시", "인천"),
            Map.entry("광주광역시", "광주"),
            Map.entry("대전광역시", "대전"),
            Map.entry("울산광역시", "울산"),
            Map.entry("세종특별자치시", "세종"),
            Map.entry("경기도", "경기"),
            Map.entry("강원특별자치도", "강원"),
            Map.entry("충청북도", "충북"),
            Map.entry("충청남도", "충남"),
            Map.entry("전북특별자치도", "전북"),
            Map.entry("전라남도", "전남"),
            Map.entry("경상북도", "경북"),
            Map.entry("경상남도", "경남"),
            Map.entry("제주특별자치도", "제주")
    );

    private SidoNameConverter() {
    }

    /**
     * 매핑 표에 없는 값이 들어오면(표기가 바뀌었거나 새 행정구역), 원본 그대로 반환.
     * 이 경우 에어코리아 호출이 실패할 수 있으니 로그로 확인 필요.
     */
    public static String toShort(String fullSidoName) {
        return FULL_TO_SHORT.getOrDefault(fullSidoName, fullSidoName);
    }
}
