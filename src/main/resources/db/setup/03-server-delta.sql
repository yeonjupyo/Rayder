-- 공용 서버 DB(likelion)에 회원가입/로그인을 적용하기 위한 델타.
--
-- 2026-08-21 기준 서버 스키마를 읽어 비교한 결과, 아래 두 가지만 부족하다.
--   1) USER 에 로그인 식별자(phone)가 없다.
--   2) USER.email 이 NOT NULL 이라 이메일을 받지 않는 회원가입이 실패한다.
-- 나머지(DAILY_UV_STATUS 의 (user_id, target_date) 유니크, SKINMON 의 user_id 유니크,
-- SKINMON_APPEARANCE 8행)는 이미 들어가 있다.
--
-- 적용:
--   mysql -h <host> -P 3306 -u <user> -p likelion < src/main/resources/db/setup/03-server-delta.sql
--
-- 주의: 02-seed-dev.sql 은 이 서버에 쓰지 말 것. 그 파일은 빈 로컬 DB 기준이라
-- DIAGNOSIS_RESULT 1번 행(현재 '복합성')을 '건성'으로 덮어써 SKINMON 외형과 어긋난다.

-- 1. 로그인 식별자와 선택 이메일
ALTER TABLE `USER`
    ADD COLUMN phone VARCHAR(20) NULL AFTER user_id,
    MODIFY COLUMN email VARCHAR(100) NULL,
    ADD CONSTRAINT uk_user_phone UNIQUE (phone);

-- 2. 기존 테스트 계정(user_id = 1)의 비밀번호가 평문이라 로그인 검증을 통과할 수 없다.
--    아래 해시는 'P@ssw0rd' 의 BCrypt 값이다. 평문일 때만 교체한다.
UPDATE `USER`
SET phone = '01000000000',
    password = '$2a$10$j4USdNRqbnQyzFUHKO2E0O4TeDTxH.r0PCcGsput5hPLuWVDrphPW'
WHERE user_id = 1
  AND password NOT LIKE '$2%';

-- 3. 요청 로그 테이블. RequestLogFilter 가 매 요청마다 여기에 쓰는데 서버에 없어서
--    지금은 매번 실패한다(비동기 + 예외 삼킴이라 응답에는 영향 없음).
--    요청 로그를 쓰지 않기로 하면 이 블록은 건너뛰고 필터를 비활성화할 것.
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
