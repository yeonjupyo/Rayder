package com.likelion.backend.notification.mapper;

import com.likelion.backend.notification.domain.NotificationSetting;
import com.likelion.backend.notification.domain.NotificationType;
import com.likelion.backend.notification.domain.NotificationWarningSetting;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {
	boolean existsUser(long userId);
	List<NotificationSetting> findAllByUserId(long userId);
	Optional<NotificationSetting> findById(long notificationId);
	Optional<NotificationSetting> findByUserIdAndType(@Param("userId") long userId,
		@Param("type") NotificationType type);
	List<LocalTime> findTimes(long notificationId);
	int insertSetting(NotificationSetting setting);
	int updateSetting(NotificationSetting setting);
	int deleteSetting(long notificationId);
	int deleteTimes(long notificationId);
	int insertTimes(@Param("notificationId") long notificationId,
		@Param("times") List<LocalTime> times);
	Optional<NotificationWarningSetting> findWarningByUserId(long userId);
	int upsertWarning(@Param("userId") long userId, @Param("enabled") boolean enabled);
}
