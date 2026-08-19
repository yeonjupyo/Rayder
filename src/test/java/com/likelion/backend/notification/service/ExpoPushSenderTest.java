package com.likelion.backend.notification.service;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.likelion.backend.notification.config.ExpoPushConfig;
import com.likelion.backend.notification.mapper.NotificationMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ExpoPushSenderTest {
	@Mock NotificationMapper mapper;

	@Test
	void sendsThroughExpoAndStoresReceiptTicket() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
		var properties = new ExpoPushConfig.Properties(true, "access-token",
			"https://exp.host/--/api/v2/push/send", "https://exp.host/--/api/v2/push/getReceipts");
		server.expect(requestTo(properties.sendUrl()))
			.andExpect(header("Authorization", "Bearer access-token"))
			.andExpect(jsonPath("$.to").value("ExpoPushToken[test-token]"))
			.andRespond(withSuccess("{\"data\":{\"status\":\"ok\",\"id\":\"receipt-id\"}}",
				MediaType.APPLICATION_JSON));

		new ExpoPushSender(restTemplate, properties, mapper).send("ExpoPushToken[test-token]",
			"title", "body", Map.of("type", "UV"));

		server.verify();
		verify(mapper).insertExpoPushTicket("receipt-id", "ExpoPushToken[test-token]");
	}
}
