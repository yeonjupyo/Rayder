package com.likelion.backend.routine.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.likelion.backend.routine.domain.RoutineType;
import com.likelion.backend.routine.dto.CareMemoCreateRequest;
import com.likelion.backend.routine.dto.RoutineCompletionRequest;
import com.likelion.backend.routine.dto.RoutineItemCreateRequest;
import com.likelion.backend.routine.dto.RoutineOrderRequest;
import java.time.LocalDate;
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
class RoutineDatabaseIntegrationTest {
	@Autowired RoutineService service;
	@Autowired JdbcTemplate jdbcTemplate;

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> "jdbc:mariadb://" + System.getenv("DB_HOST") + ":"
			+ System.getenv("DB_PORT") + "/" + System.getenv("DB_NAME"));
		registry.add("spring.datasource.username", () -> System.getenv("DB_USERNAME"));
		registry.add("spring.datasource.password", () -> System.getenv("DB_PASSWORD"));
	}

	@Test
	void managesRoutinesDateCompletionsSoftDeletesAndMemos() {
		long userId = insertUser();
		LocalDate date = LocalDate.of(2026, 8, 19);
		var morning = service.createRoutine(userId, RoutineType.MORNING);
		service.createRoutine(userId, RoutineType.EVENING);
		var first = service.addItem(morning.routineId(), userId,
			new RoutineItemCreateRequest("세안", "미온수"));
		var second = service.addItem(morning.routineId(), userId,
			new RoutineItemCreateRequest("토너", null));
		service.reorder(morning.routineId(), userId, new RoutineOrderRequest(List.of(second.id(), first.id())));
		service.updateCompletion(first.id(), userId, new RoutineCompletionRequest(date, true));
		var memo = service.addMemo(userId, new CareMemoCreateRequest(date, "선크림 구매"));
		service.updateMemoCompletion(memo.id(), userId, true);

		var beforeDelete = service.findAll(userId, date);
		assertThat(beforeDelete.morning().items()).extracting("id").containsExactly(second.id(), first.id());
		assertThat(beforeDelete.completedCount()).isEqualTo(1);
		assertThat(beforeDelete.progressRate()).isEqualTo(50);
		assertThat(beforeDelete.memos()).singleElement().extracting("done").isEqualTo(true);

		service.deleteItem(first.id(), userId);
		var afterDelete = service.findAll(userId, date);
		assertThat(afterDelete.morning().items()).extracting("id").containsExactly(second.id());
		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM ROUTINE_ITEM_COMPLETION WHERE item_id = ?", Integer.class, first.id()))
			.isEqualTo(1);
	}

	private long insertUser() {
		String email = "routine-test-" + UUID.randomUUID() + "@example.com";
		jdbcTemplate.update("INSERT INTO `USER` (email, password, nickname) VALUES (?, ?, ?)",
			email, "test-password", "routine-test");
		return jdbcTemplate.queryForObject("SELECT user_id FROM `USER` WHERE email = ?", Long.class, email);
	}
}
