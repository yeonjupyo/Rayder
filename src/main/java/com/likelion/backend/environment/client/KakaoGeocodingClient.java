package com.likelion.backend.environment.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.likelion.backend.environment.config.EnvironmentApiConfig;
import com.likelion.backend.environment.exception.EnvironmentApiException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class KakaoGeocodingClient {
	private static final String BASE_URL = "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json";
	public record GeoRegion(String sido, String gugun, String dong) { }
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final EnvironmentApiConfig.Properties properties;

	public KakaoGeocodingClient(@Qualifier("environmentRestTemplate") RestTemplate restTemplate,
		ObjectMapper objectMapper, EnvironmentApiConfig.Properties properties) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	public GeoRegion resolveRegion(double lat, double lon) {
		validateCoordinates(lat, lon);
		URI uri = UriComponentsBuilder.fromUriString(BASE_URL).queryParam("x", lon).queryParam("y", lat)
			.build().encode().toUri();
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "KakaoAK " + properties.kakaoRestApiKey());
		try {
			String body = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
			JsonNode documents = objectMapper.readTree(body).path("documents");
			if (!documents.isArray() || documents.isEmpty())
				throw EnvironmentApiException.regionNotFound("No administrative region for the coordinates");
			JsonNode target = null;
			for (JsonNode document : documents) if ("B".equals(document.path("region_type").asText())) { target = document; break; }
			if (target == null) target = documents.get(0);
			String sido = target.path("region_1depth_name").asText();
			String gugun = target.path("region_2depth_name").asText();
			if (sido.isBlank() || gugun.isBlank()) throw new EnvironmentApiException("Kakao response has no sido/gugun");
			return new GeoRegion(sido, gugun, target.path("region_3depth_name").asText(""));
		} catch (EnvironmentApiException e) { throw e;
		} catch (Exception e) { throw new EnvironmentApiException("Kakao geocoding API call failed", e); }
	}

	private void validateCoordinates(double lat, double lon) {
		if (!Double.isFinite(lat) || !Double.isFinite(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180)
			throw EnvironmentApiException.invalidInput("lat must be -90..90 and lon must be -180..180");
	}
}
