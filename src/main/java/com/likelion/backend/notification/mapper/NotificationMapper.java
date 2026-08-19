package com.likelion.backend.notification.mapper;

import com.likelion.backend.notification.domain.NotificationSetting;
import com.likelion.backend.notification.domain.NotificationType;
import com.likelion.backend.notification.domain.NotificationWarningSetting;
import com.likelion.backend.notification.domain.DevicePlatform;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {
	record DeliveryTarget(long userId, NotificationType type, String token, String sido, String gugun) { }
	record WarningTarget(long userId, String token, String sido, String gugun) { }
	record Location(String sido, String gugun) { }
	record ExpoTicket(String receiptId, String token) { }
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
	int upsertDeviceToken(@Param("userId") long userId, @Param("token") String token,
		@Param("platform") DevicePlatform platform);
	int deactivateDeviceToken(@Param("userId") long userId, @Param("token") String token);
	int upsertLocation(@Param("userId") long userId, @Param("sido") String sido,
		@Param("gugun") String gugun);
	Optional<Location> findLocation(long userId);
	List<DeliveryTarget> findDueScheduledTargets(@Param("time") LocalTime time);
	List<WarningTarget> findWarningTargets();
	int insertWarningDeliveryIfAbsent(@Param("userId") long userId,
		@Param("forecastAt") java.time.LocalDateTime forecastAt);
	int deactivateToken(String token);
	int insertExpoPushTicket(@Param("receiptId") String receiptId, @Param("token") String token);
	List<ExpoTicket> findPendingExpoPushTickets();
	int markExpoPushTicketChecked(String receiptId);
}
