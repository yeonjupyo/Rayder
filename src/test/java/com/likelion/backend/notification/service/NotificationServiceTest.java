package com.likelion.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion.backend.common.exception.BusinessException;
import com.likelion.backend.notification.domain.NotificationSetting;
import com.likelion.backend.notification.domain.NotificationType;
import com.likelion.backend.notification.dto.NotificationSettingRequest;
import com.likelion.backend.notification.dto.NotificationUpdateRequest;
import com.likelion.backend.notification.mapper.NotificationMapper;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
	@Mock NotificationMapper mapper;
	NotificationService service;

	@BeforeEach
	void setUp() {
		service = new NotificationService(mapper);
	}

	@Test
	void listsUserSettingsAndDefaultWarning() {
		NotificationSetting setting = setting(1L, 7L, NotificationType.UV, true);
		when(mapper.existsUser(7L)).thenReturn(true);
		when(mapper.findAllByUserId(7L)).thenReturn(List.of(setting));
		when(mapper.findTimes(1L)).thenReturn(List.of(LocalTime.of(9, 0)));
		when(mapper.findWarningByUserId(7L)).thenReturn(Optional.empty());

		var response = service.findAll(7L);

		assertThat(response.notifications()).hasSize(1);
		assertThat(response.notifications().get(0).times()).containsExactly("09:00");
		assertThat(response.uvExposureWarning().enabled()).isFalse();
	}

	@Test
	void createsSettingWithMultipleTimes() {
		when(mapper.existsUser(7L)).thenReturn(true);
		when(mapper.findByUserIdAndType(7L, NotificationType.ROUTINE)).thenReturn(Optional.empty());
		when(mapper.insertSetting(any())).thenAnswer(invocation -> {
			setId(invocation.getArgument(0), 3L); return 1;
		});
		NotificationSetting saved = setting(3L, 7L, NotificationType.ROUTINE, true);
		when(mapper.findById(3L)).thenReturn(Optional.of(saved));
		when(mapper.findTimes(3L)).thenReturn(List.of(LocalTime.of(8, 0), LocalTime.of(21, 0)));

		var response = service.create(7L,
			new NotificationSettingRequest(NotificationType.ROUTINE, true, List.of("21:00", "08:00")));

		assertThat(response.times()).containsExactly("08:00", "21:00");
		verify(mapper).insertTimes(eq(3L), eq(List.of(LocalTime.of(8, 0), LocalTime.of(21, 0))));
	}

	@Test
	void changesEnabledStateAndReplacesTimes() {
		NotificationSetting setting = setting(1L, 7L, NotificationType.DUST, true);
		when(mapper.findById(1L)).thenReturn(Optional.of(setting));
		when(mapper.findTimes(1L)).thenReturn(List.of(LocalTime.of(10, 0)));

		var response = service.update(1L, 7L,
			new NotificationUpdateRequest(false, List.of("10:00")));

		assertThat(response.enabled()).isFalse();
		verify(mapper).deleteTimes(1L);
		verify(mapper).insertTimes(1L, List.of(LocalTime.of(10, 0)));
	}

	@Test
	void acceptsEmptyTimeList() {
		when(mapper.findById(1L)).thenReturn(Optional.of(setting(1L, 7L, NotificationType.UV, true)));
		when(mapper.findTimes(1L)).thenReturn(List.of());

		service.update(1L, 7L, new NotificationUpdateRequest(false, List.of()));

		verify(mapper).deleteTimes(1L);
		verify(mapper, never()).insertTimes(anyLong(), any());
	}

	@Test
	void rejectsInvalidTime() {
		when(mapper.findById(1L)).thenReturn(Optional.of(setting(1L, 7L, NotificationType.UV, true)));
		assertThatThrownBy(() -> service.update(1L, 7L,
			new NotificationUpdateRequest(true, List.of("24:00"))))
			.isInstanceOf(BusinessException.class).extracting("code").isEqualTo("INVALID_NOTIFICATION_TIME");
	}

	@Test
	void rejectsDuplicateTime() {
		when(mapper.findById(1L)).thenReturn(Optional.of(setting(1L, 7L, NotificationType.UV, true)));
		assertThatThrownBy(() -> service.update(1L, 7L,
			new NotificationUpdateRequest(true, List.of("09:00", "09:00"))))
			.isInstanceOf(BusinessException.class).extracting("code").isEqualTo("DUPLICATE_NOTIFICATION_TIME");
	}

	@Test
	void rejectsMissingSetting() {
		when(mapper.findById(99L)).thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.delete(99L, 7L))
			.isInstanceOf(BusinessException.class).extracting("code").isEqualTo("NOTIFICATION_NOT_FOUND");
	}

	@Test
	void preventsAnotherUserAccess() {
		when(mapper.findById(1L)).thenReturn(Optional.of(setting(1L, 8L, NotificationType.UV, true)));
		assertThatThrownBy(() -> service.delete(1L, 7L))
			.isInstanceOf(BusinessException.class).extracting("code").isEqualTo("NOTIFICATION_FORBIDDEN");
	}

	private NotificationSetting setting(long id, long userId, NotificationType type, boolean enabled) {
		return NotificationSetting.builder().notificationId(id).userId(userId).type(type).enabled(enabled).build();
	}

	private void setId(NotificationSetting setting, long id) {
		try {
			var field = NotificationSetting.class.getDeclaredField("notificationId");
			field.setAccessible(true);
			field.set(setting, id);
		} catch (ReflectiveOperationException exception) {
			throw new AssertionError(exception);
		}
	}
}
