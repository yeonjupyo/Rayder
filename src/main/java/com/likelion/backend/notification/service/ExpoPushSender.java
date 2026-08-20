package com.likelion.backend.notification.service;

import com.likelion.backend.notification.config.ExpoPushConfig;
import com.likelion.backend.notification.mapper.NotificationMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.expo.enabled", havingValue = "true")
public class ExpoPushSender implements PushSender {
	private final RestTemplate restTemplate;
	private final ExpoPushConfig.Properties properties;
	private final NotificationMapper mapper;

	public ExpoPushSender(@Qualifier("expoPushRestTemplate") RestTemplate restTemplate,
		ExpoPushConfig.Properties properties, NotificationMapper mapper) {
		this.restTemplate = restTemplate;
		this.properties = properties;
		this.mapper = mapper;
	}

	@Override
	public void send(String token, String title, String body, Map<String, String> data) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("to", token);
		payload.put("sound", "default");
		payload.put("title", title);
		payload.put("body", body);
		payload.put("data", data);
		JsonNode response = restTemplate.postForObject(properties.sendUrl(),
			new HttpEntity<>(payload, headers()), JsonNode.class);
		JsonNode ticket = response == null ? null : response.path("data");
		if (ticket != null && ticket.isArray()) ticket = ticket.path(0);
		if (ticket == null || ticket.isMissingNode()) throw new IllegalStateException("Expo returned no push ticket");
		if ("ok".equals(ticket.path("status").asText())) {
			mapper.insertExpoPushTicket(ticket.path("id").asText(), token);
			return;
		}
		String error = ticket.path("details").path("error").asText();
		if ("DeviceNotRegistered".equals(error)) mapper.deactivateToken(token);
		throw new IllegalStateException("Expo rejected push notification: " + error);
	}

	HttpHeaders headers() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
		if (properties.accessToken() != null && !properties.accessToken().isBlank())
			headers.setBearerAuth(properties.accessToken());
		return headers;
	}
}
