-- Apply manually to MariaDB. Stores only the current notification region, not location history.
CREATE TABLE DEVICE_TOKEN (
    device_token_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    token VARCHAR(512) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (device_token_id),
    CONSTRAINT uk_device_token_token UNIQUE (token),
    CONSTRAINT fk_device_token_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE,
    INDEX idx_device_token_user_active (user_id, is_active)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE NOTIFICATION_LOCATION (
    user_id INT NOT NULL,
    sido VARCHAR(30) NOT NULL,
    gugun VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_notification_location_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE NOTIFICATION_WARNING_DELIVERY (
    user_id INT NOT NULL,
    forecast_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, forecast_at),
    CONSTRAINT fk_warning_delivery_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE EXPO_PUSH_TICKET (
    receipt_id VARCHAR(100) NOT NULL,
    token VARCHAR(512) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checked_at DATETIME NULL,
    PRIMARY KEY (receipt_id),
    INDEX idx_expo_push_ticket_pending (checked_at, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
