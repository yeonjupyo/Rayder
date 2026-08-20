package com.likelion.backend.environment.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.likelion.backend.environment.config.EnvironmentApiConfig;
import com.likelion.backend.environment.dto.EnvironmentInfo;
import com.likelion.backend.environment.exception.EnvironmentApiException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AirKoreaDustClient {
	private static final String BASE_URL = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty";
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final EnvironmentApiConfig.Properties properties;

	public AirKoreaDustClient(@Qualifier("environmentRestTemplate") RestTemplate restTemplate,
		ObjectMapper objectMapper, EnvironmentApiConfig.Properties properties) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	public EnvironmentInfo getPm10(String sido, String district, String region) {
		JsonNode item = findStationItem(sido, district);
		double value = parseValue(item, "pm10Value");
		return new EnvironmentInfo(EnvironmentInfo.Type.DUST_PM10, value,
			resolveLevel(item.path("pm10Grade").asText(), value, 30, 80, 150), region, LocalDateTime.now());
	}

	public EnvironmentInfo getPm25(String sido, String district, String region) {
		JsonNode item = findStationItem(sido, district);
		double value = parseValue(item, "pm25Value");
		return new EnvironmentInfo(EnvironmentInfo.Type.DUST_PM25, value,
			resolveLevel(item.path("pm25Grade").asText(), value, 15, 35, 75), region, LocalDateTime.now());
	}

	private JsonNode findStationItem(String sido, String district) {
		URI uri = URI.create(BASE_URL
			+ "?serviceKey=" + encode(properties.dataGoKrServiceKey())
			+ "&returnType=json&numOfRows=100&pageNo=1"
			+ "&sidoName=" + encode(sido) + "&ver=1.0");
		try {
			JsonNode root = objectMapper.readTree(restTemplate.getForObject(uri, String.class));
			JsonNode header = root.path("response").path("header");
			if (!"00".equals(header.path("resultCode").asText()))
				throw new EnvironmentApiException("AirKorea API error: " + header.path("resultMsg").asText());
			JsonNode items = root.path("response").path("body").path("items");
			if (!items.isArray() || items.isEmpty()) throw new EnvironmentApiException("AirKorea response has no stations");
			for (JsonNode item : items)
				if (item.path("stationName").asText("").contains(district)) return item;
			throw EnvironmentApiException.regionNotFound("No AirKorea station matched district: " + district);
		} catch (EnvironmentApiException e) { throw e;
		} catch (Exception e) { throw new EnvironmentApiException("AirKorea API call failed (" + e.getClass().getSimpleName() + ")", e); }
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private double parseValue(JsonNode item, String field) {
		String raw = item.path(field).asText();
		try { return Double.parseDouble(raw); }
		catch (RuntimeException e) { throw new EnvironmentApiException("AirKorea response has invalid " + field, e); }
	}

	private String resolveLevel(String grade, double value, double good, double normal, double bad) {
		return switch (grade) {
			case "1" -> "좋음"; case "2" -> "보통"; case "3" -> "나쁨"; case "4" -> "매우나쁨";
			default -> value <= good ? "좋음" : value <= normal ? "보통" : value <= bad ? "나쁨" : "매우나쁨";
		};
	}
}
