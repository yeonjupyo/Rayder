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
 * 프론트엔드가 다른 오리진에서 호출하므로 preflight 가 실제로 통과하는지 고정한다.
 * MockMvc 대신 실제 포트를 띄우고 JDK HttpClient 로 OPTIONS 를 보낸다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties = "web.cors.allowed-origins=http://localhost:5173")
class WebCorsConfigTest {

	@LocalServerPort
	int port;

	private HttpResponse<Void> preflight(String origin) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/home"))
			.method("OPTIONS", HttpRequest.BodyPublishers.noBody())
			.header("Origin", origin)
			.header("Access-Control-Request-Method", "GET")
			.build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
	}

	@Test
	void allowsPreflightFromTheConfiguredOrigin() throws Exception {
		HttpResponse<Void> response = preflight("http://localhost:5173");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.headers().firstValue("Access-Control-Allow-Origin"))
			.contains("http://localhost:5173");
	}

	@Test
	void rejectsPreflightFromAnUnknownOrigin() throws Exception {
		HttpResponse<Void> response = preflight("http://evil.example.com");

		assertThat(response.statusCode()).isEqualTo(403);
		assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
	}
}
