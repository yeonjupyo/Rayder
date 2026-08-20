package com.example.uvmate.environment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.uvmate.dto.UvForecastPoint;
import com.example.uvmate.environment.config.EnvironmentApiConfig;
import com.example.uvmate.environment.dto.EnvironmentInfo;
import com.example.uvmate.environment.exception.EnvironmentApiException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 기상청_생활기상지수 조회서비스(3.0) - getUVIdxV5
 *
 * 주의: 이 API는 위경도가 아니라 "areaNo"(동네예보 지점코드)로 조회한다.
 * areaNo 조회는 RegionResolver(kma-area-codes.csv 기반)가 구/군 이름으로
 * 미리 해결해서 넘겨준다고 가정.
 *
 * BASE_URL은 V4->V5 버전업 네이밍 규칙을 따른 추정값. 공공데이터포털
 * 활용신청 상세페이지의 실제 "요청주소"와 다르면 그 값으로 교체할 것.
 */
@Component
public class KmaUvClient {

    private static final String BASE_URL = "https://apis.data.go.kr/1360000/LivingWthrIdxServiceV5/getUVIdxV5";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final RestTemplate restTemplate;
    private final EnvironmentApiConfig apiConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KmaUvClient(RestTemplate environmentRestTemplate, EnvironmentApiConfig apiConfig) {
        this.restTemplate = environmentRestTemplate;
        this.apiConfig = apiConfig;
    }

    /**
     * @param areaNo   기상청 동네예보 지점코드 (예: 강남구 -> 미리 매핑해둔 코드)
     * @param regionLabel 사용자에게 보여줄 지역명 (예: "서울시 강남구")
     */
    public EnvironmentInfo getUvIndex(String areaNo, String regionLabel) {
        String time = LocalDateTime.now().minusHours(3).format(TIME_FORMAT);

        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("serviceKey", apiConfig.getServiceKey())
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 10)
                .queryParam("dataType", "JSON")
                .queryParam("areaNo", areaNo)
                .queryParam("time", time)
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
                        "기상청 UV API 오류: " + header.path("resultMsg").asText());
            }

            JsonNode items = root.path("response").path("body").path("items").path("item");
            if (!items.isArray() || items.isEmpty()) {
                throw new EnvironmentApiException("기상청 UV API 응답에 조회 결과가 없음 (areaNo=" + areaNo + ")");
            }

            JsonNode first = items.get(0);
            double value = first.path("h0").asDouble();

            return new EnvironmentInfo(
                    EnvironmentInfo.Type.UV,
                    value,
                    resolveUvLevel(value),
                    regionLabel,
                    LocalDateTime.now()
            );

        } catch (EnvironmentApiException e) {
            throw e;
        } catch (Exception e) {
            throw new EnvironmentApiException("기상청 UV API 호출/파싱 실패", e);
        }
    }

    /**
     * 오늘 하루치 시간대별 UV 예보 (0~24시간 후, 3시간 간격: h0,h3,...,h24)
     */
    public List<UvForecastPoint> getUvForecastToday(String areaNo) {
        String time = LocalDateTime.now().minusHours(3).format(TIME_FORMAT);

        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("serviceKey", apiConfig.getServiceKey())
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 10)
                .queryParam("dataType", "JSON")
                .queryParam("areaNo", areaNo)
                .queryParam("time", time)
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
                        "기상청 UV API 오류: " + header.path("resultMsg").asText());
            }

            JsonNode items = root.path("response").path("body").path("items").path("item");
            if (!items.isArray() || items.isEmpty()) {
                throw new EnvironmentApiException("기상청 UV API 응답에 조회 결과가 없음 (areaNo=" + areaNo + ")");
            }

            JsonNode first = items.get(0);
            List<UvForecastPoint> forecast = new ArrayList<>();
            for (int h = 0; h <= 24; h += 3) {
                String fieldName = "h" + h;
                if (first.has(fieldName)) {
                    forecast.add(new UvForecastPoint(h, first.path(fieldName).asDouble()));
                }
            }
            return forecast;

        } catch (EnvironmentApiException e) {
            throw e;
        } catch (Exception e) {
            throw new EnvironmentApiException("기상청 UV 예보 API 호출/파싱 실패", e);
        }
    }

    private String resolveUvLevel(double value) {
        if (value < 3) return "낮음";
        if (value <= 5) return "보통";
        if (value <= 7) return "높음";
        if (value <= 10) return "매우높음";
        return "위험";
    }
}