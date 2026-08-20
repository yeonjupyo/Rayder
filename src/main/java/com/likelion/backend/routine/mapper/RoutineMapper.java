package com.likelion.backend.routine.mapper;

import com.likelion.backend.routine.domain.CareMemo;
import com.likelion.backend.routine.domain.RoutineItem;
import com.likelion.backend.routine.domain.RoutineType;
import com.likelion.backend.routine.domain.UserRoutine;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoutineMapper {
	boolean existsUser(long userId);
	List<UserRoutine> findRoutinesByUserId(long userId);
	Optional<UserRoutine> findRoutineById(long routineId);
	Optional<UserRoutine> findRoutineByUserIdAndType(@Param("userId") long userId, @Param("type") RoutineType type);
	int insertRoutine(UserRoutine routine);
	List<RoutineItem> findItemsForDate(@Param("routineId") long routineId, @Param("date") LocalDate date);
	List<RoutineItem> findActiveItems(long routineId);
	Optional<RoutineItem> findItemById(long itemId);
	int insertItem(RoutineItem item);
	int updateItem(@Param("itemId") long itemId, @Param("name") String name, @Param("detail") String detail);
	int softDeleteItem(long itemId);
	int moveItemTemporarily(@Param("itemId") long itemId, @Param("stepOrder") int stepOrder);
	int updateItemOrder(@Param("itemId") long itemId, @Param("stepOrder") int stepOrder);
	int upsertCompletion(@Param("itemId") long itemId, @Param("date") LocalDate date,
		@Param("completed") boolean completed);
	List<CareMemo> findMemos(@Param("userId") long userId, @Param("date") LocalDate date);
	Optional<CareMemo> findMemoById(long memoId);
	int insertMemo(CareMemo memo);
	int updateMemoContent(@Param("memoId") long memoId, @Param("content") String content);
	int updateMemoCompletion(@Param("memoId") long memoId, @Param("completed") boolean completed);
	int deleteMemo(long memoId);
}
