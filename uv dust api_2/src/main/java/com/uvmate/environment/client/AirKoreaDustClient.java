package com.uvmate.environment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uvmate.environment.config.EnvironmentApiConfig;
import com.uvmate.environment.dto.EnvironmentInfo;
import com.uvmate.environment.exception.EnvironmentApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 한국환경공단_에어코리아_대기오염정보 - getCtprvnRltmMesureDnsty
 * (시도별 실시간 측정정보 조회)
 *
 * 정확한 측정소명을 몰라도 시도명("서울")만 알면 그 시도 전체 측정소 목록을
 * 한번에 받아올 수 있음. 그 목록 안에서 사용자가 찾는 구 이름과 일치하는
 * 측정소를 찾아서 쓰는 방식으로 변경.
 *
 * (측정소명이 항상 "OO구"와 정확히 일치하지 않는 지역도 있을 수 있어서,
 *  나중에 위경도 기반 근접측정소 조회로 더 정교하게 바꿀 수도 있음 - 지금은
 *  시도 단위로 받아서 구 이름으로 필터링하는 단순한 버전)
 */
@Component
public class AirKoreaDustClient {

    private static final String BASE_URL =
            "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty";

    private final RestTemplate restTemplate;
    private final EnvironmentApiConfig apiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AirKoreaDustClient(RestTemplate environmentRestTemplate, EnvironmentApiConfig apiConfig) {
        this.restTemplate = environmentRestTemplate;
        this.apiConfig = apiConfig;
    }

    /**
     * @param sidoName       시도명 (예: "서울", "부산" ... 전국 가능)
     * @param districtKeyword 찾고 싶은 구/군 이름 (예: "강남구")
     * @param regionLabel    사용자에게 보여줄 지역명 (예: "서울시 강남구")
     */
    public EnvironmentInfo getPm10(String sidoName, String districtKeyword, String regionLabel) {
        JsonNode item = findStationItem(sidoName, districtKeyword);
        double pm10 = item.path("pm10Value").asDouble();
        String grade = item.path("pm10Grade").asText("");

        return new EnvironmentInfo(
                EnvironmentInfo.Type.DUST_PM10,
                pm10,
                grade.isEmpty() ? resolvePm10Level(pm10) : gradeToLevel(grade),
                regionLabel,
                LocalDateTime.now()
        );
    }

    public EnvironmentInfo getPm25(String sidoName, String districtKeyword, String regionLabel) {
        JsonNode item = findStationItem(sidoName, districtKeyword);
        double pm25 = item.path("pm25Value").asDouble();
        String grade = item.path("pm25Grade").asText("");

        return new EnvironmentInfo(
                EnvironmentInfo.Type.DUST_PM25,
                pm25,
                grade.isEmpty() ? resolvePm25Level(pm25) : gradeToLevel(grade),
                regionLabel,
                LocalDateTime.now()
        );
    }

    /**
     * API가 내려주는 공식 등급값(1~4)을 텍스트로 변환.
     * 문서 "항목별 Grade 값의 의미" 기준: 1=좋음, 2=보통, 3=나쁨, 4=매우나쁨.
     * pm10Grade/pm25Grade가 빈 값으로 오는 경우도 있어서, 그럴 땐 직접 계산한
     * resolvePm10Level/resolvePm25Level로 대체(폴백).
     */
    private String gradeToLevel(String grade) {
        return switch (grade) {
            case "1" -> "좋음";
            case "2" -> "보통";
            case "3" -> "나쁨";
            case "4" -> "매우나쁨";
            default -> "정보없음";
        };
    }

    /**
     * 시도 전체 측정소 목록을 받아온 뒤, stationName에 districtKeyword가 포함된
     * 첫 번째 측정소를 반환. 같은 구 안에 측정소가 여러 개인 지역도 있어서
     * "포함" 매칭으로 처리 (완전 일치로 하면 못 찾는 경우가 생김).
     */
    private JsonNode findStationItem(String sidoName, String districtKeyword) {
        // 중요: serviceKey에 +, = 문자가 포함돼 있어서 build().encode()로
        // 퍼센트 인코딩 필수. 안 하면 서버가 에러코드 30을 던짐
        // ("서비스키를 URL 인코딩하지 않음" - 문서 2장 OpenAPI 에러코드정리 참고).
        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("serviceKey", apiConfig.getServiceKey())
                .queryParam("returnType", "json")
                .queryParam("numOfRows", 100)
                .queryParam("pageNo", 1)
                .queryParam("sidoName", sidoName)
                .queryParam("ver", "1.0")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        try {
            String rawResponse = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(rawResponse);

            JsonNode header = root.path("response").path("header");
            String resultCode = header.path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                throw new EnvironmentApiException(
                        "에어코리아 API 오류: " + header.path("resultMsg").asText());
            }

            JsonNode items = root.path("response").path("body").path("items");
            if (!items.isArray() || items.isEmpty()) {
                throw new EnvironmentApiException("에어코리아 API 응답에 측정소 목록 없음 (sido=" + sidoName + ")");
            }

            for (JsonNode item : items) {
                String stationName = item.path("stationName").asText("");
                if (stationName.contains(districtKeyword)) {
                    return item;
                }
            }

            throw new EnvironmentApiException(
                    "'" + sidoName + "' 시도 목록에서 '" + districtKeyword + "'와 일치하는 측정소를 찾지 못함");

        } catch (EnvironmentApiException e) {
            throw e;
        } catch (Exception e) {
            throw new EnvironmentApiException("에어코리아 API 호출/파싱 실패", e);
        }
    }

    private String resolvePm10Level(double value) {
        if (value <= 30) return "좋음";
        if (value <= 80) return "보통";
        if (value <= 150) return "나쁨";
        return "매우나쁨";
    }

    private String resolvePm25Level(double value) {
        if (value <= 15) return "좋음";
        if (value <= 35) return "보통";
        if (value <= 75) return "나쁨";
        return "매우나쁨";
    }
}

