package com.likelion.backend.environment.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.likelion.backend.environment.config.EnvironmentApiConfig;
import com.likelion.backend.environment.dto.EnvironmentInfo;
import com.likelion.backend.environment.exception.EnvironmentApiException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;

@Component
public class KmaUvClient {
	private static final String BASE_URL = "https://apis.data.go.kr/1360000/LivingWthrIdxServiceV5/getUVIdxV5";
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH");
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final EnvironmentApiConfig.Properties properties;

	public KmaUvClient(@Qualifier("environmentRestTemplate") RestTemplate restTemplate,
		ObjectMapper objectMapper, EnvironmentApiConfig.Properties properties) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	public EnvironmentInfo getUvIndex(String areaNo, String regionLabel) {
		String time = LocalDateTime.now().minusHours(3).format(TIME_FORMAT);
		URI uri = URI.create(BASE_URL
			+ "?serviceKey=" + encode(properties.dataGoKrServiceKey())
			+ "&pageNo=1&numOfRows=10&dataType=JSON"
			+ "&areaNo=" + encode(areaNo) + "&time=" + encode(time));
		try {
			JsonNode root = objectMapper.readTree(restTemplate.getForObject(uri, String.class));
			JsonNode header = root.path("response").path("header");
			if (!"00".equals(header.path("resultCode").asText()))
				throw new EnvironmentApiException("KMA UV API error: " + header.path("resultMsg").asText());
			JsonNode items = root.path("response").path("body").path("items").path("item");
			if (!items.isArray() || items.isEmpty()) throw new EnvironmentApiException("KMA UV response has no result");
			double value = items.get(0).path("h0").asDouble();
			return new EnvironmentInfo(EnvironmentInfo.Type.UV, value, level(value), regionLabel, LocalDateTime.now());
		} catch (EnvironmentApiException e) { throw e;
		} catch (Exception e) { throw new EnvironmentApiException("KMA UV API call failed (" + e.getClass().getSimpleName() + ")", e); }
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private String level(double value) {
		if (value < 3) return "낮음";
		if (value <= 5) return "보통";
		if (value <= 7) return "높음";
		if (value <= 10) return "매우높음";
		return "위험";
	}
}
