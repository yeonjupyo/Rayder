-- 신규 DB 를 처음부터 만들 때 쓰는 전체 스키마.
--
-- db/migration/V1~V5 는 이미 provisioning 된 DB 를 변경해 온 "이력"이고, USER · 진단 · 스킨몽 ·
-- 홈 · 챗봇 테이블은 어느 마이그레이션에도 생성문이 없다(수동 생성된 상태). 그래서 이 파일은
-- 마이그레이션 이력을 다시 재생하는 대신 V1~V5 를 모두 적용한 "최종 형태"를 한 번에 만든다.
--
-- 적용:
--   mysql -u root -p -e "CREATE DATABASE hackathon DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci"
--   mysql -u root -p hackathon < src/main/resources/db/setup/01-schema.sql
--   mysql -u root -p hackathon < src/main/resources/db/setup/02-seed-dev.sql
--
-- 이미 돌아가는 DB 에 이 파일을 그대로 적용하면 안 된다(CREATE TABLE IF NOT EXISTS 라 기존
-- 테이블은 건너뛰지만, 컬럼이 다르면 조용히 어긋난 상태가 된다). 기존 DB 와 대조가 필요하면
-- mysqldump --no-data 로 뽑아 이 파일과 비교할 것.

-- ---------------------------------------------------------------------------
-- 사용자
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `USER` (
    user_id    INT          NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL,
    -- 실제 로그인이 붙기 전까지는 사용되지 않는다. 붙일 때 해시(BCrypt)만 저장할 것.
    password   VARCHAR(255) NULL,
    nickname   VARCHAR(50)  NOT NULL,
    region     VARCHAR(100) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_user_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 피부진단
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS DIAGNOSIS_ANSWER (
    answer_id        INT          NOT NULL AUTO_INCREMENT,
    user_id          INT          NOT NULL,
    question_no      INT          NOT NULL,
    question_content VARCHAR(255) NOT NULL,
    answer_value     VARCHAR(20)  NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (answer_id),
    CONSTRAINT fk_diagnosis_answer_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE,
    INDEX idx_diagnosis_answer_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS DIAGNOSIS_RESULT (
    result_id      INT          NOT NULL AUTO_INCREMENT,
    user_id        INT          NOT NULL,
    skin_type      VARCHAR(20)  NOT NULL,
    result_summary VARCHAR(255) NULL,
    diagnosed_at   DATETIME     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (result_id),
    CONSTRAINT fk_diagnosis_result_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE,
    -- AI 추천이 최신 1건을 ORDER BY diagnosed_at DESC, result_id DESC 로 고른다.
    INDEX idx_diagnosis_result_user_latest (user_id, diagnosed_at, result_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 스킨몽
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS SKINMON_APPEARANCE (
    appearance_id   INT          NOT NULL AUTO_INCREMENT,
    skin_type       VARCHAR(20)  NOT NULL,
    expression_type VARCHAR(20)  NOT NULL,
    image_url       VARCHAR(255) NULL,
    PRIMARY KEY (appearance_id),
    CONSTRAINT uk_skinmon_appearance_type UNIQUE (skin_type, expression_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS SKINMON (
    skinmon_id    INT         NOT NULL AUTO_INCREMENT,
    user_id       INT         NOT NULL,
    result_id     INT         NOT NULL,
    skinmon_name  VARCHAR(50) NOT NULL,
    appearance_id INT         NOT NULL,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (skinmon_id),
    -- HomeMapper.findSkinTypeByUserId 가 단일 행을 기대하므로 한 사용자당 하나로 제한한다.
    -- 여러 마리를 허용하는 정책으로 바꾸려면 이 제약을 지우고 해당 쿼리도 최신 1건만 고르도록 고쳐야 한다.
    CONSTRAINT uk_skinmon_user UNIQUE (user_id),
    CONSTRAINT fk_skinmon_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_skinmon_result FOREIGN KEY (result_id) REFERENCES DIAGNOSIS_RESULT (result_id),
    CONSTRAINT fk_skinmon_appearance FOREIGN KEY (appearance_id) REFERENCES SKINMON_APPEARANCE (appearance_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 홈 화면 (일자별 자외선 노출량)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS DAILY_UV_STATUS (
    status_id     INT           NOT NULL AUTO_INCREMENT,
    user_id       INT           NOT NULL,
    target_date   DATE          NOT NULL,
    uv_index      DECIMAL(5, 1) NULL,
    dust_index    INT           NULL,
    exposure_rate DECIMAL(8, 4) NULL,
    max_uv_today  DECIMAL(5, 1) NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (status_id),
    -- HomeMapper.upsertTodayUvStatus 의 ON DUPLICATE KEY UPDATE 가 이 제약에 의존한다.
    -- 없으면 홈 조회마다 행이 쌓이고 findTodayUvStatus 가 TooManyResults 로 터진다.
    CONSTRAINT uk_daily_uv_status_user_date UNIQUE (user_id, target_date),
    CONSTRAINT fk_daily_uv_status_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 챗봇
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS CHATBOT_CONVERSATION (
    conversation_id INT      NOT NULL AUTO_INCREMENT,
    user_id         INT      NOT NULL,
    started_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (conversation_id),
    CONSTRAINT fk_chatbot_conversation_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE,
    INDEX idx_chatbot_conversation_user (user_id, started_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS CHATBOT_MESSAGE (
    message_id      INT      NOT NULL AUTO_INCREMENT,
    conversation_id INT      NOT NULL,
    sender_type     VARCHAR(10) NOT NULL,
    message_content TEXT     NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (message_id),
    CONSTRAINT ck_chatbot_message_sender CHECK (sender_type IN ('USER', 'BOT')),
    CONSTRAINT fk_chatbot_message_conversation FOREIGN KEY (conversation_id)
        REFERENCES CHATBOT_CONVERSATION (conversation_id) ON DELETE CASCADE,
    INDEX idx_chatbot_message_conversation (conversation_id, message_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 루틴 · 케어메모 (V4 적용 후 최종 형태)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS USER_ROUTINE (
    routine_id INT         NOT NULL AUTO_INCREMENT,
    user_id    INT         NOT NULL,
    time_type  VARCHAR(20) NOT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (routine_id),
    CONSTRAINT uk_user_routine_user_type UNIQUE (user_id, time_type),
    CONSTRAINT ck_user_routine_time_type CHECK (time_type IN ('MORNING', 'EVENING')),
    CONSTRAINT fk_user_routine_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ROUTINE_ITEM (
    item_id           INT          NOT NULL AUTO_INCREMENT,
    routine_id        INT          NOT NULL,
    item_name         VARCHAR(50)  NOT NULL,
    description       VARCHAR(100) NULL,
    step_order        INT          NOT NULL,
    is_ai_recommended TINYINT(1)   NOT NULL DEFAULT 0,
    deleted_at        DATETIME     NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (item_id),
    CONSTRAINT fk_routine_item_routine FOREIGN KEY (routine_id)
        REFERENCES USER_ROUTINE (routine_id) ON DELETE CASCADE,
    INDEX idx_routine_item_routine_order (routine_id, step_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ROUTINE_ITEM_COMPLETION (
    completion_id   INT        NOT NULL AUTO_INCREMENT,
    item_id         INT        NOT NULL,
    completion_date DATE       NOT NULL,
    is_completed    TINYINT(1) NOT NULL,
    created_at      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (completion_id),
    CONSTRAINT uk_routine_item_completion_item_date UNIQUE (item_id, completion_date),
    CONSTRAINT fk_routine_item_completion_item FOREIGN KEY (item_id) REFERENCES ROUTINE_ITEM (item_id),
    INDEX idx_routine_item_completion_date (completion_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS CARE_MEMO (
    memo_id      INT          NOT NULL AUTO_INCREMENT,
    user_id      INT          NOT NULL,
    target_date  DATE         NOT NULL,
    content      VARCHAR(255) NOT NULL,
    is_completed TINYINT(1)   NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (memo_id),
    CONSTRAINT fk_care_memo_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE,
    INDEX idx_care_memo_user_date (user_id, target_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 알림 (V3 · V5 적용 후 최종 형태)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS NOTIFICATION_SETTING (
    noti_id    INT         NOT NULL AUTO_INCREMENT,
    user_id    INT         NOT NULL,
    noti_type  VARCHAR(20) NOT NULL,
    is_active  TINYINT(1)  NOT NULL DEFAULT 1,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (noti_id),
    CONSTRAINT uk_notification_setting_user_type UNIQUE (user_id, noti_type),
    CONSTRAINT ck_notification_setting_type CHECK (noti_type IN ('UV', 'DUST', 'ROUTINE')),
    CONSTRAINT fk_notification_setting_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE,
    INDEX idx_notification_setting_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS NOTIFICATION_TIME (
    notification_time_id INT      NOT NULL AUTO_INCREMENT,
    noti_id              INT      NOT NULL,
    alert_time           TIME     NOT NULL,
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_time_id),
    CONSTRAINT uk_notification_time_setting_time UNIQUE (noti_id, alert_time),
    CONSTRAINT fk_notification_time_setting FOREIGN KEY (noti_id)
        REFERENCES NOTIFICATION_SETTING (noti_id) ON DELETE CASCADE,
    INDEX idx_notification_time_setting (noti_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS NOTIFICATION_WARNING_SETTING (
    user_id    INT        NOT NULL,
    is_active  TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_notification_warning_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS DEVICE_TOKEN (
    device_token_id BIGINT      NOT NULL AUTO_INCREMENT,
    user_id         INT         NOT NULL,
    token           VARCHAR(512) NOT NULL,
    platform        VARCHAR(20) NOT NULL,
    is_active       TINYINT(1)  NOT NULL DEFAULT 1,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (device_token_id),
    CONSTRAINT uk_device_token_token UNIQUE (token),
    CONSTRAINT fk_device_token_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE,
    INDEX idx_device_token_user_active (user_id, is_active)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS NOTIFICATION_LOCATION (
    user_id    INT         NOT NULL,
    sido       VARCHAR(30) NOT NULL,
    gugun      VARCHAR(30) NOT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_notification_location_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS NOTIFICATION_WARNING_DELIVERY (
    user_id     INT      NOT NULL,
    forecast_at DATETIME NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, forecast_at),
    CONSTRAINT fk_warning_delivery_user FOREIGN KEY (user_id) REFERENCES `USER` (user_id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS EXPO_PUSH_TICKET (
    receipt_id VARCHAR(100) NOT NULL,
    token      VARCHAR(512) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checked_at DATETIME     NULL,
    PRIMARY KEY (receipt_id),
    INDEX idx_expo_push_ticket_pending (checked_at, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 요청 로그 (V2 와 동일)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS request (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    uuid          CHAR(36)      NOT NULL,
    ip            VARCHAR(64)   NOT NULL,
    user_agent    JSON          NOT NULL,
    method        VARCHAR(10)   NOT NULL,
    host          VARCHAR(255)  NULL,
    url           VARCHAR(255)  NULL,
    body          JSON          NULL,
    query         JSON          NULL,
    params        VARCHAR(1024) NULL,
    headers       VARCHAR(4096) NULL,
    cookies       VARCHAR(2048) NULL,
    response_body JSON          NULL,
    error         TEXT          NULL,
    status        INT           NULL,
    duration      INT           NULL,
    request_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    response_at   DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at    DATETIME(6)   NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_request_uuid (uuid),
    KEY idx_request_created_at (created_at),
    KEY idx_request_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
