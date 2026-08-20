package com.likelion.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.likelion.backend.auth.controller.UserController;
import com.likelion.backend.chat.controller.ChatController;
import com.likelion.backend.diagnosis.controller.DiagnosisController;
import com.likelion.backend.home.controller.HomeController;
import com.likelion.backend.skinmon.controller.SkinmonController;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest
class BackendApplicationTests {
	@Autowired ApplicationContext context;
	@Autowired SqlSessionFactory sqlSessionFactory;
	@Autowired RequestMappingHandlerMapping requestMappingHandlerMapping;

	@Test
	void allFeaturePackagesAndDiagnosisStatementsAreRegistered() {
		assertThat(context.getBean(UserController.class)).isNotNull();
		assertThat(context.getBean(DiagnosisController.class)).isNotNull();
		assertThat(context.getBean(SkinmonController.class)).isNotNull();
		assertThat(context.getBean(ChatController.class)).isNotNull();
		assertThat(context.getBean(HomeController.class)).isNotNull();

		var configuration = sqlSessionFactory.getConfiguration();
		assertThat(configuration.hasStatement(
			"com.likelion.backend.diagnosis.mapper.DiagnosisMapper.insertAnswers")).isTrue();
		assertThat(configuration.hasStatement(
			"com.likelion.backend.diagnosis.mapper.DiagnosisMapper.insertResult")).isTrue();
	}

	@Test
	void allFeatureRoutesAreRegisteredInOneContext() {
		Set<String> routes = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
			.flatMap(mapping -> mapping.getPatternValues().stream())
			.collect(Collectors.toSet());

		assertThat(routes).contains(
			"/api/auth/login",
			"/api/diagnosis/submit",
			"/api/skinmon",
			"/api/chat",
			"/api/home",
			"/api/environment/uv",
			"/api/environment/dust",
			"/api/environment/uv/by-location",
			"/api/environment/dust/by-location",
			"/api/location",
			"/api/routines",
			"/api/routines/from-ai",
			"/api/routines/{routineId}/items",
			"/api/routine-items/{itemId}",
			"/api/routines/{routineId}/items/order",
			"/api/routine-items/{itemId}/completion",
			"/api/care-memos",
			"/api/care-memos/{memoId}",
			"/api/care-memos/{memoId}/completion",
			"/api/notifications",
			"/api/notifications/{notificationId}",
			"/api/notifications/uv-risk-warning",
			"/api/notifications/devices",
			"/api/notifications/location",
			"/api/ai-routines/recommend"
		);
	}
}
