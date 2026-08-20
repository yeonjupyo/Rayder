-- 회원가입/로그인 도입. 로그인 식별자는 휴대폰 번호(숫자만)이고 비밀번호는 BCrypt 해시로 저장한다.
-- 이미 운영 중인 DB 에 수동 적용한다. 새 DB 는 db/setup/01-schema.sql 이 최종 형태로 만든다.

ALTER TABLE `USER`
    ADD COLUMN phone VARCHAR(20) NULL AFTER user_id,
    -- 회원가입은 이메일을 받지 않는다.
    MODIFY COLUMN email VARCHAR(255) NULL,
    MODIFY COLUMN password VARCHAR(255) NULL,
    ADD CONSTRAINT uk_user_phone UNIQUE (phone);
