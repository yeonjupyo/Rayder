package com.likelion.backend.notification.service;

import com.likelion.backend.notification.config.ExpoPushConfig;
import com.likelion.backend.notification.mapper.NotificationMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

@Component
@ConditionalOnProperty(name = "notification.expo.enabled", havingValue = "true")
public class ExpoPushReceiptScheduler {
	private final RestTemplate restTemplate;
	private final ExpoPushConfig.Properties properties;
	private final NotificationMapper mapper;

	public ExpoPushReceiptScheduler(@Qualifier("expoPushRestTemplate") RestTemplate restTemplate,
		ExpoPushConfig.Properties properties, NotificationMapper mapper) {
		this.restTemplate = restTemplate;
		this.properties = properties;
		this.mapper = mapper;
	}

	@Scheduled(cron = "0 */15 * * * *", zone = "Asia/Seoul")
	public void checkReceipts() {
		var tickets = mapper.findPendingExpoPushTickets();
		if (tickets.isEmpty()) return;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		if (properties.accessToken() != null && !properties.accessToken().isBlank())
			headers.setBearerAuth(properties.accessToken());
		JsonNode response = restTemplate.postForObject(properties.receiptsUrl(),
			new HttpEntity<>(Map.of("ids", tickets.stream().map(NotificationMapper.ExpoTicket::receiptId).toList()), headers),
			JsonNode.class);
		if (response == null) return;
		JsonNode receipts = response.path("data");
		for (var ticket : tickets) {
			JsonNode receipt = receipts.path(ticket.receiptId());
			if (receipt.isMissingNode()) continue;
			if ("DeviceNotRegistered".equals(receipt.path("details").path("error").asText()))
				mapper.deactivateToken(ticket.token());
			mapper.markExpoPushTicketChecked(ticket.receiptId());
		}
	}
}
