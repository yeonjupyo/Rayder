package com.uvmate.environment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uvmate.environment.exception.EnvironmentApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * 카카오 로컬 API - 좌표로 행정구역정보 변환 (coord2regioncode)
 * https://dapi.kakao.com/v2/local/geo/coord2regioncode.json?x={경도}&y={위도}
 *
 * data.go.kr 키와는 완전히 별개의 카카오 REST API 키가 필요함
 * (카카오 디벨로퍼스에서 애플리케이션 등록 후 발급).
 *
 * 응답의 documents 배열은 보통 2개(행정동/법정동)가 오는데,
 * region_type "B"(법정동) 쪽이 기상청/에어코리아 행정구역 코드 체계와 더 잘 맞아서 그걸 사용.
 */
@Component
public class KakaoGeocodingClient {

    private static final String BASE_URL = "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json";

    public record GeoRegion(String sido, String gugun, String dong) {
    }

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    public KakaoGeocodingClient(RestTemplate environmentRestTemplate) {
        this.restTemplate = environmentRestTemplate;
    }

    /**
     * @param lat 위도 (y)
     * @param lon 경도 (x)
     */
    public GeoRegion resolveRegion(double lat, double lon) {
        URI uri = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .queryParam("x", lon)
                .queryParam("y", lat)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode documents = root.path("documents");
            if (!documents.isArray() || documents.isEmpty()) {
                throw new EnvironmentApiException("카카오 좌표->행정구역 변환 결과 없음 (lat=" + lat + ", lon=" + lon + ")");
            }

            // 법정동(B) 기준 문서를 우선 사용. 없으면 첫 번째 문서로 대체.
            JsonNode target = null;
            for (JsonNode doc : documents) {
                if ("B".equals(doc.path("region_type").asText())) {
                    target = doc;
                    break;
                }
            }
            if (target == null) {
                target = documents.get(0);
            }

            String sido = target.path("region_1depth_name").asText();
            String gugun = target.path("region_2depth_name").asText();
            String dong = target.path("region_3depth_name").asText("");

            if (sido.isEmpty() || gugun.isEmpty()) {
                throw new EnvironmentApiException("카카오 응답에 시도/구군 정보 없음 (lat=" + lat + ", lon=" + lon + ")");
            }

            return new GeoRegion(sido, gugun, dong);

        } catch (EnvironmentApiException e) {
            throw e;
        } catch (Exception e) {
            throw new EnvironmentApiException("카카오 좌표->행정구역 API 호출/파싱 실패", e);
        }
    }
}
