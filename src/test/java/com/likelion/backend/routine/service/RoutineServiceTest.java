package com.likelion.backend.routine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion.backend.ai.dto.AiRoutineItem;
import com.likelion.backend.ai.dto.AiRoutineSaveRequest;
import com.likelion.backend.common.exception.BusinessException;
import com.likelion.backend.routine.domain.RoutineItem;
import com.likelion.backend.routine.domain.RoutineType;
import com.likelion.backend.routine.domain.UserRoutine;
import com.likelion.backend.routine.dto.RoutineCompletionRequest;
import com.likelion.backend.routine.dto.RoutineOrderRequest;
import com.likelion.backend.routine.mapper.RoutineMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutineServiceTest {
	@Mock RoutineMapper mapper;
	RoutineService service;

	@BeforeEach
	void setUp() {
		service = new RoutineService(mapper);
	}

	@Test
	void readsBothRoutinesAndCalculatesDateProgress() {
		LocalDate date = LocalDate.of(2026, 8, 19);
		when(mapper.existsUser(7L)).thenReturn(true);
		when(mapper.findRoutinesByUserId(7L)).thenReturn(List.of(
			routine(1L, 7L, RoutineType.MORNING), routine(2L, 7L, RoutineType.EVENING)));
		when(mapper.findItemsForDate(1L, date)).thenReturn(List.of(item(10L, 1L, 7L, 1, true)));
		when(mapper.findItemsForDate(2L, date)).thenReturn(List.of(item(11L, 2L, 7L, 1, false)));
		when(mapper.findMemos(7L, date)).thenReturn(List.of());

		var response = service.findAll(7L, date);

		assertThat(response.morning().items()).hasSize(1);
		assertThat(response.evening().items()).hasSize(1);
		assertThat(response.completedCount()).isEqualTo(1);
		assertThat(response.totalCount()).isEqualTo(2);
		assertThat(response.progressRate()).isEqualTo(50);
	}

	@Test
	void rejectsMalformedDate() {
		assertThatThrownBy(() -> service.parseDate("2026-02-30"))
			.isInstanceOf(BusinessException.class)
			.hasMessageContaining("yyyy-MM-dd");
	}

	@Test
	void rejectsCrossUserCompletion() {
		when(mapper.findItemById(10L)).thenReturn(Optional.of(item(10L, 1L, 8L, 1, false)));

		assertThatThrownBy(() -> service.updateCompletion(10L, 7L,
			new RoutineCompletionRequest(LocalDate.of(2026, 8, 19), true)))
			.isInstanceOf(BusinessException.class)
			.extracting("code").isEqualTo("ROUTINE_ITEM_FORBIDDEN");
		verify(mapper, never()).upsertCompletion(10L, LocalDate.of(2026, 8, 19), true);
	}

	@Test
	void completionUsesDateUpsert() {
		LocalDate date = LocalDate.of(2026, 8, 19);
		when(mapper.findItemById(10L)).thenReturn(Optional.of(item(10L, 1L, 7L, 1, false)));

		var response = service.updateCompletion(10L, 7L, new RoutineCompletionRequest(date, true));

		verify(mapper).upsertCompletion(10L, date, true);
		assertThat(response.done()).isTrue();
	}

	@Test
	void rejectsOrderMissingAnActiveItem() {
		when(mapper.findRoutineById(1L)).thenReturn(Optional.of(routine(1L, 7L, RoutineType.MORNING)));
		when(mapper.findActiveItems(1L)).thenReturn(List.of(
			item(10L, 1L, 7L, 1, false), item(11L, 1L, 7L, 2, false)));

		assertThatThrownBy(() -> service.reorder(1L, 7L, new RoutineOrderRequest(List.of(10L))))
			.isInstanceOf(BusinessException.class)
			.extracting("code").isEqualTo("INVALID_ROUTINE_ORDER");
	}

	@Test
	void deletingItemIsSoftAndResequencesRemainingItems() {
		when(mapper.findItemById(10L)).thenReturn(Optional.of(item(10L, 1L, 7L, 1, false)));
		when(mapper.findActiveItems(1L)).thenReturn(List.of(item(11L, 1L, 7L, 2, false)));

		service.deleteItem(10L, 7L);

		verify(mapper).softDeleteItem(10L);
		verify(mapper).updateItemOrder(11L, 1);
	}

	@Test
	void aiSaveConvertsSelectionToGeneralRoutineAndSkipsExactDuplicates() {
		when(mapper.existsUser(7L)).thenReturn(true);
		when(mapper.findRoutineByUserIdAndType(7L, RoutineType.MORNING))
			.thenReturn(Optional.of(routine(1L, 7L, RoutineType.MORNING)));
		when(mapper.findRoutineByUserIdAndType(7L, RoutineType.EVENING))
			.thenReturn(Optional.of(routine(2L, 7L, RoutineType.EVENING)));
		when(mapper.findActiveItems(1L)).thenReturn(
			List.of(itemNamed(10L, 1L, 7L, 1, "세안")),
			List.of(itemNamed(10L, 1L, 7L, 1, "세안")));
		when(mapper.findActiveItems(2L)).thenReturn(List.of(), List.of());
		AiRoutineSaveRequest request = new AiRoutineSaveRequest(List.of(
			new AiRoutineItem(1, "세안", "중복 항목"), new AiRoutineItem(2, "토너", "피부결 정돈")),
			List.of(new AiRoutineItem(1, "크림", "보습 마무리")));

		service.saveFromAi(7L, request);

		org.mockito.ArgumentCaptor<RoutineItem> captor = org.mockito.ArgumentCaptor.forClass(RoutineItem.class);
		verify(mapper, org.mockito.Mockito.times(2)).insertItem(captor.capture());
		assertThat(captor.getAllValues()).extracting(RoutineItem::getName).containsExactly("토너", "크림");
		assertThat(captor.getAllValues()).allMatch(RoutineItem::isAiRecommended);
	}

	@Test
	void aiSaveRejectsNonConsecutiveOrderBeforeWriting() {
		when(mapper.existsUser(7L)).thenReturn(true);
		AiRoutineSaveRequest request = new AiRoutineSaveRequest(
			List.of(new AiRoutineItem(2, "토너", "피부결 정돈")), List.of());

		assertThatThrownBy(() -> service.saveFromAi(7L, request)).isInstanceOf(BusinessException.class)
			.extracting("code").isEqualTo("INVALID_AI_ROUTINE");
		verify(mapper, never()).insertItem(org.mockito.ArgumentMatchers.any());
	}

	private UserRoutine routine(long id, long userId, RoutineType type) {
		return UserRoutine.builder().routineId(id).userId(userId).type(type).build();
	}

	private RoutineItem item(long id, long routineId, long userId, int order, boolean completed) {
		return RoutineItem.builder().itemId(id).routineId(routineId).userId(userId)
			.name("item-" + id).stepOrder(order).completed(completed).build();
	}

	private RoutineItem itemNamed(long id, long routineId, long userId, int order, String name) {
		return RoutineItem.builder().itemId(id).routineId(routineId).userId(userId)
			.name(name).stepOrder(order).build();
	}
}
