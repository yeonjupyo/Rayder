package com.likelion.backend.environment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.likelion.backend.environment.client.KakaoGeocodingClient;
import com.likelion.backend.environment.config.EnvironmentApiConfig;
import com.likelion.backend.environment.exception.EnvironmentApiException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

class KakaoGeocodingClientTest {
	private final KakaoGeocodingClient client = new KakaoGeocodingClient(new RestTemplate(), new ObjectMapper(),
		new EnvironmentApiConfig.Properties("data-key", "kakao-key"));

	@Test
	void rejectsInvalidLatitudeBeforeCallingKakao() {
		assertThatThrownBy(() -> client.resolveRegion(91, 127.0))
			.isInstanceOf(EnvironmentApiException.class)
			.extracting(e -> ((EnvironmentApiException) e).getStatus().value()).isEqualTo(400);
	}

	@Test
	void rejectsInvalidLongitudeBeforeCallingKakao() {
		assertThatThrownBy(() -> client.resolveRegion(37.0, 181))
			.isInstanceOf(EnvironmentApiException.class)
			.extracting(e -> ((EnvironmentApiException) e).getStatus().value()).isEqualTo(400);
	}
}
