package com.likelion.backend.notification.service;

import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.expo.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledPushSender implements PushSender {
	@Override public void send(String token, String title, String body, Map<String, String> data) {
		throw new IllegalStateException("Expo Push delivery is disabled; set EXPO_PUSH_ENABLED=true");
	}
}
