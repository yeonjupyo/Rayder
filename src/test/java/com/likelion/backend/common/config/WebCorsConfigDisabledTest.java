package com.likelion.backend.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * 운영 프로파일은 WEB_CORS_ALLOWED_ORIGINS 를 비워두는 것이 기본값이다(같은 오리진 서빙 전제).
 * 빈 값이 빈 문자열 오리진 하나로 바인딩돼 이상한 허용이 생기지 않는지 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties = "web.cors.allowed-origins=")
class WebCorsConfigDisabledTest {

	@LocalServerPort
	int port;

	@Test
	void doesNotAllowAnyOriginWhenTheListIsEmpty() throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/home"))
			.method("OPTIONS", HttpRequest.BodyPublishers.noBody())
			.header("Origin", "http://localhost:5173")
			.header("Access-Control-Request-Method", "GET")
			.build();

		HttpResponse<Void> response = HttpClient.newHttpClient()
			.send(request, HttpResponse.BodyHandlers.discarding());

		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
	}
}
