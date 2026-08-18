-- Notification settings agreed on 2026-08-19.
-- NOTIFICATION_SETTING was empty when this migration was prepared.
-- Scheduled notification types: UV, DUST, ROUTINE.
-- The cumulative UV exposure warning is an independent, untimed preference.

ALTER TABLE NOTIFICATION_SETTING
    DROP COLUMN alert_time,
    DROP COLUMN is_predictive,
    MODIFY COLUMN noti_type VARCHAR(20) NOT NULL,
    MODIFY COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1,
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD CONSTRAINT uk_notification_setting_user_type UNIQUE (user_id, noti_type),
    ADD CONSTRAINT ck_notification_setting_type CHECK (noti_type IN ('UV', 'DUST', 'ROUTINE')),
    ADD CONSTRAINT fk_notification_setting_user
        FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE;

CREATE INDEX idx_notification_setting_user
    ON NOTIFICATION_SETTING (user_id);

CREATE TABLE NOTIFICATION_TIME (
    notification_time_id INT NOT NULL AUTO_INCREMENT,
    noti_id INT NOT NULL,
    alert_time TIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_time_id),
    CONSTRAINT uk_notification_time_setting_time UNIQUE (noti_id, alert_time),
    CONSTRAINT fk_notification_time_setting
        FOREIGN KEY (noti_id) REFERENCES NOTIFICATION_SETTING (noti_id) ON DELETE CASCADE,
    INDEX idx_notification_time_setting (noti_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE NOTIFICATION_WARNING_SETTING (
    user_id INT NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_notification_warning_user
        FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
