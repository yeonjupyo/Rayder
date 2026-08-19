package com.likelion.backend.routine.service;

import com.likelion.backend.common.exception.BusinessException;
import com.likelion.backend.routine.domain.CareMemo;
import com.likelion.backend.routine.domain.RoutineItem;
import com.likelion.backend.routine.domain.RoutineType;
import com.likelion.backend.routine.domain.UserRoutine;
import com.likelion.backend.routine.dto.CareMemoCreateRequest;
import com.likelion.backend.routine.dto.CareMemoResponse;
import com.likelion.backend.routine.dto.MyRoutineResponse;
import com.likelion.backend.routine.dto.RoutineCompletionRequest;
import com.likelion.backend.routine.dto.RoutineGroupResponse;
import com.likelion.backend.routine.dto.RoutineItemCreateRequest;
import com.likelion.backend.routine.dto.RoutineItemResponse;
import com.likelion.backend.routine.dto.RoutineItemUpdateRequest;
import com.likelion.backend.routine.dto.RoutineOrderRequest;
import com.likelion.backend.routine.mapper.RoutineMapper;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {
	private final RoutineMapper mapper;

	public LocalDate parseDate(String value) {
		try {
			return LocalDate.parse(value);
		} catch (DateTimeParseException exception) {
			throw new BusinessException("INVALID_DATE", "Date must use yyyy-MM-dd format", HttpStatus.BAD_REQUEST);
		}
	}

	public MyRoutineResponse findAll(long userId, LocalDate date) {
		validateUser(userId);
		List<UserRoutine> routines = mapper.findRoutinesByUserId(userId);
		RoutineGroupResponse morning = group(routines, RoutineType.MORNING, date);
		RoutineGroupResponse evening = group(routines, RoutineType.EVENING, date);
		List<CareMemoResponse> memos = mapper.findMemos(userId, date).stream().map(this::memoResponse).toList();
		List<RoutineItemResponse> all = java.util.stream.Stream.concat(
			morning.items().stream(), evening.items().stream()).toList();
		int completed = (int) all.stream().filter(RoutineItemResponse::done).count();
		int total = all.size();
		int progress = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
		return new MyRoutineResponse(date, morning, evening, memos, completed, total, progress);
	}

	@Transactional
	public RoutineGroupResponse createRoutine(long userId, RoutineType type) {
		validateUser(userId);
		if (mapper.findRoutineByUserIdAndType(userId, type).isPresent()) {
			throw new BusinessException("ROUTINE_ALREADY_EXISTS", "Routine already exists for type " + type,
				HttpStatus.CONFLICT);
		}
		UserRoutine routine = UserRoutine.builder().userId(userId).type(type).build();
		try {
			mapper.insertRoutine(routine);
		} catch (DuplicateKeyException exception) {
			throw new BusinessException("ROUTINE_ALREADY_EXISTS", "Routine already exists for type " + type,
				HttpStatus.CONFLICT);
		}
		return new RoutineGroupResponse(routine.getRoutineId(), type, List.of());
	}

	@Transactional
	public RoutineItemResponse addItem(long routineId, long userId, RoutineItemCreateRequest request) {
		requireOwnedRoutine(routineId, userId);
		int order = mapper.findActiveItems(routineId).size() + 1;
		RoutineItem item = RoutineItem.builder().routineId(routineId).name(request.name().trim())
			.detail(trimNullable(request.detail())).stepOrder(order).aiRecommended(false).build();
		mapper.insertItem(item);
		return new RoutineItemResponse(item.getItemId(), item.getName(), item.getDetail(), false, order);
	}

	@Transactional
	public RoutineItemResponse updateItem(long itemId, long userId, RoutineItemUpdateRequest request) {
		RoutineItem item = requireOwnedActiveItem(itemId, userId);
		String name = request.name().trim();
		String detail = trimNullable(request.detail());
		mapper.updateItem(itemId, name, detail);
		return new RoutineItemResponse(itemId, name, detail, false, item.getStepOrder());
	}

	@Transactional
	public void deleteItem(long itemId, long userId) {
		RoutineItem item = requireOwnedActiveItem(itemId, userId);
		mapper.softDeleteItem(itemId);
		resequence(mapper.findActiveItems(item.getRoutineId()));
	}

	@Transactional
	public List<RoutineItemResponse> reorder(long routineId, long userId, RoutineOrderRequest request) {
		requireOwnedRoutine(routineId, userId);
		List<RoutineItem> active = mapper.findActiveItems(routineId);
		List<Long> currentIds = active.stream().map(RoutineItem::getItemId).toList();
		if (request.itemIds().size() != active.size()
			|| new HashSet<>(request.itemIds()).size() != request.itemIds().size()
			|| !new HashSet<>(request.itemIds()).equals(new HashSet<>(currentIds))) {
			throw new BusinessException("INVALID_ROUTINE_ORDER",
				"itemIds must contain every active item exactly once", HttpStatus.BAD_REQUEST);
		}
		for (int index = 0; index < request.itemIds().size(); index++) {
			mapper.moveItemTemporarily(request.itemIds().get(index), -(index + 1));
		}
		for (int index = 0; index < request.itemIds().size(); index++) {
			mapper.updateItemOrder(request.itemIds().get(index), index + 1);
		}
		return mapper.findActiveItems(routineId).stream().map(this::itemResponse).toList();
	}

	@Transactional
	public RoutineItemResponse updateCompletion(long itemId, long userId, RoutineCompletionRequest request) {
		RoutineItem item = requireOwnedActiveItem(itemId, userId);
		mapper.upsertCompletion(itemId, request.date(), request.completed());
		return new RoutineItemResponse(itemId, item.getName(), item.getDetail(), request.completed(),
			item.getStepOrder());
	}

	@Transactional
	public CareMemoResponse addMemo(long userId, CareMemoCreateRequest request) {
		validateUser(userId);
		CareMemo memo = CareMemo.builder().userId(userId).targetDate(request.date())
			.content(request.content().trim()).completed(false).build();
		mapper.insertMemo(memo);
		return requireOwnedMemo(memo.getMemoId(), userId, false);
	}

	@Transactional
	public CareMemoResponse updateMemo(long memoId, long userId, String content) {
		requireOwnedMemo(memoId, userId, true);
		mapper.updateMemoContent(memoId, content.trim());
		return requireOwnedMemo(memoId, userId, false);
	}

	@Transactional
	public CareMemoResponse updateMemoCompletion(long memoId, long userId, boolean completed) {
		requireOwnedMemo(memoId, userId, true);
		mapper.updateMemoCompletion(memoId, completed);
		return requireOwnedMemo(memoId, userId, false);
	}

	@Transactional
	public void deleteMemo(long memoId, long userId) {
		requireOwnedMemo(memoId, userId, true);
		mapper.deleteMemo(memoId);
	}

	private RoutineGroupResponse group(List<UserRoutine> routines, RoutineType type, LocalDate date) {
		return routines.stream().filter(routine -> routine.getType() == type).findFirst()
			.map(routine -> new RoutineGroupResponse(routine.getRoutineId(), type,
				mapper.findItemsForDate(routine.getRoutineId(), date).stream().map(this::itemResponse).toList()))
			.orElse(new RoutineGroupResponse(null, type, List.of()));
	}

	private UserRoutine requireOwnedRoutine(long routineId, long userId) {
		UserRoutine routine = mapper.findRoutineById(routineId).orElseThrow(() ->
			new BusinessException("ROUTINE_NOT_FOUND", "Routine not found: " + routineId, HttpStatus.NOT_FOUND));
		if (!routine.getUserId().equals(userId)) {
			throw new BusinessException("ROUTINE_FORBIDDEN", "Routine belongs to another user", HttpStatus.FORBIDDEN);
		}
		return routine;
	}

	private RoutineItem requireOwnedActiveItem(long itemId, long userId) {
		RoutineItem item = mapper.findItemById(itemId).orElseThrow(() ->
			new BusinessException("ROUTINE_ITEM_NOT_FOUND", "Routine item not found: " + itemId, HttpStatus.NOT_FOUND));
		if (!item.getUserId().equals(userId)) {
			throw new BusinessException("ROUTINE_ITEM_FORBIDDEN", "Routine item belongs to another user", HttpStatus.FORBIDDEN);
		}
		if (item.getDeletedAt() != null) {
			throw new BusinessException("ROUTINE_ITEM_NOT_FOUND", "Routine item not found: " + itemId, HttpStatus.NOT_FOUND);
		}
		return item;
	}

	private CareMemoResponse requireOwnedMemo(long memoId, long userId, boolean ownershipOnly) {
		CareMemo memo = mapper.findMemoById(memoId).orElseThrow(() ->
			new BusinessException("CARE_MEMO_NOT_FOUND", "Care memo not found: " + memoId, HttpStatus.NOT_FOUND));
		if (!memo.getUserId().equals(userId)) {
			throw new BusinessException("CARE_MEMO_FORBIDDEN", "Care memo belongs to another user", HttpStatus.FORBIDDEN);
		}
		return ownershipOnly ? null : memoResponse(memo);
	}

	private void validateUser(long userId) {
		if (!mapper.existsUser(userId)) {
			throw new BusinessException("USER_NOT_FOUND", "User not found: " + userId, HttpStatus.NOT_FOUND);
		}
	}

	private void resequence(List<RoutineItem> items) {
		for (int index = 0; index < items.size(); index++) {
			mapper.updateItemOrder(items.get(index).getItemId(), index + 1);
		}
	}

	private RoutineItemResponse itemResponse(RoutineItem item) {
		return new RoutineItemResponse(item.getItemId(), item.getName(), item.getDetail(),
			item.isCompleted(), item.getStepOrder());
	}

	private CareMemoResponse memoResponse(CareMemo memo) {
		return new CareMemoResponse(memo.getMemoId(), memo.getTargetDate(), memo.getContent(),
			memo.isCompleted(), memo.getCreatedAt(), memo.getUpdatedAt());
	}

	private String trimNullable(String value) {
		return value == null ? null : value.trim();
	}
}
