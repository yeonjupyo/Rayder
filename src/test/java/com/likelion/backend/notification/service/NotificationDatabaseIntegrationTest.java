package com.likelion.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.likelion.backend.notification.domain.NotificationType;
import com.likelion.backend.notification.dto.NotificationSettingRequest;
import com.likelion.backend.notification.dto.NotificationUpdateRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_TEST_ENABLED", matches = "true")
class NotificationDatabaseIntegrationTest {
	@Autowired NotificationService service;
	@Autowired JdbcTemplate jdbcTemplate;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> "jdbc:mariadb://" + System.getenv("DB_HOST") + ":"
			+ System.getenv("DB_PORT") + "/" + System.getenv("DB_NAME"));
		registry.add("spring.datasource.username", () -> System.getenv("DB_USERNAME"));
		registry.add("spring.datasource.password", () -> System.getenv("DB_PASSWORD"));
	}

	@Test
	void persistsListsUpdatesAndDeletesSettingsAtomically() {
		long userId = insertUser();

		var created = service.create(userId,
			new NotificationSettingRequest(NotificationType.ROUTINE, true, List.of("08:00", "21:00")));
		assertThat(service.findAll(userId).notifications()).hasSize(1);
		assertThat(created.times()).containsExactly("08:00", "21:00");

		var disabled = service.update(created.notificationId(), userId,
			new NotificationUpdateRequest(false, List.of("09:00")));
		assertThat(disabled.enabled()).isFalse();
		assertThat(disabled.times()).containsExactly("09:00");

		var enabled = service.update(created.notificationId(), userId,
			new NotificationUpdateRequest(true, List.of("09:00", "18:00")));
		assertThat(enabled.enabled()).isTrue();
		assertThat(enabled.times()).containsExactly("09:00", "18:00");

		assertThat(service.updateWarning(userId, true).enabled()).isTrue();
		service.delete(created.notificationId(), userId);
		assertThat(service.findAll(userId).notifications()).isEmpty();
	}

	private long insertUser() {
		String email = "notification-test-" + UUID.randomUUID() + "@example.com";
		jdbcTemplate.update("INSERT INTO `USER` (email, password, nickname) VALUES (?, ?, ?)",
			email, "test-password", "notification-test");
		return jdbcTemplate.queryForObject("SELECT user_id FROM `USER` WHERE email = ?", Long.class, email);
	}
}
