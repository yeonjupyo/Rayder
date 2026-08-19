-- Permanent user routines with date-specific completion records.
ALTER TABLE USER_ROUTINE
    DROP COLUMN target_date,
    DROP COLUMN progress_rate,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD CONSTRAINT uk_user_routine_user_type UNIQUE (user_id, time_type),
    ADD CONSTRAINT ck_user_routine_time_type CHECK (time_type IN ('MORNING', 'EVENING')),
    ADD CONSTRAINT fk_user_routine_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE;

ALTER TABLE ROUTINE_ITEM
    DROP COLUMN is_completed,
    MODIFY COLUMN is_ai_recommended TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN deleted_at DATETIME NULL,
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

CREATE TABLE ROUTINE_ITEM_COMPLETION (
    completion_id INT NOT NULL AUTO_INCREMENT,
    item_id INT NOT NULL,
    completion_date DATE NOT NULL,
    is_completed TINYINT(1) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (completion_id),
    CONSTRAINT uk_routine_item_completion_item_date UNIQUE (item_id, completion_date),
    CONSTRAINT fk_routine_item_completion_item FOREIGN KEY (item_id)
        REFERENCES ROUTINE_ITEM (item_id),
    INDEX idx_routine_item_completion_date (completion_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

ALTER TABLE CARE_MEMO
    MODIFY COLUMN is_completed TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD CONSTRAINT fk_care_memo_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE,
    ADD INDEX idx_care_memo_user_date (user_id, target_date);
