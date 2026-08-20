-- 개발·데모용 시드 데이터. 여러 번 실행해도 같은 상태가 되도록 만들었다.
--
-- 적용: mysql -u root -p hackathon < src/main/resources/db/setup/02-seed-dev.sql
--
-- 담고 있는 것
--   1. 1번 테스트 사용자 (POST /api/auth/login 이 무조건 이 계정을 반환한다)
--   2. SKINMON_APPEARANCE 참조 데이터 (없으면 POST /api/skinmon 이 항상 실패한다)
--   3. 프론트가 바로 화면을 그릴 수 있는 최소 상태: 진단 결과 1건, 스킨몽 1마리,
--      아침·저녁 루틴과 항목, 케어메모, 알림 설정

-- ---------------------------------------------------------------------------
-- 1. 테스트 사용자
-- ---------------------------------------------------------------------------
-- 로그인 정보: 휴대폰 01000000000 / 비밀번호 P@ssw0rd
-- password 는 BCrypt 해시다. 개발용 계정이므로 그대로 두고, 운영 데이터에는 절대 쓰지 말 것.
INSERT INTO `USER` (user_id, phone, email, nickname, region, password)
VALUES (1, '01000000000', 'test@example.com', '테스터', '서울특별시 강남구',
        '$2a$10$j4USdNRqbnQyzFUHKO2E0O4TeDTxH.r0PCcGsput5hPLuWVDrphPW')
ON DUPLICATE KEY UPDATE phone = VALUES(phone), nickname = VALUES(nickname),
                        region = VALUES(region), password = VALUES(password);

-- ---------------------------------------------------------------------------
-- 2. 스킨몽 외형 참조 데이터
--    DiagnosisService 가 판정하는 네 가지 피부타입 × 표정 두 가지.
--    표정은 HomeService 가 노출률 80% 기준으로 happy / sad 를 고른다.
-- ---------------------------------------------------------------------------
INSERT INTO SKINMON_APPEARANCE (skin_type, expression_type, image_url) VALUES
    ('건성',  'happy', '/assets/skinmon/dry-happy.png'),
    ('건성',  'sad',   '/assets/skinmon/dry-sad.png'),
    ('지성',  'happy', '/assets/skinmon/oily-happy.png'),
    ('지성',  'sad',   '/assets/skinmon/oily-sad.png'),
    ('복합성', 'happy', '/assets/skinmon/combination-happy.png'),
    ('복합성', 'sad',   '/assets/skinmon/combination-sad.png'),
    ('민감성', 'happy', '/assets/skinmon/sensitive-happy.png'),
    ('민감성', 'sad',   '/assets/skinmon/sensitive-sad.png')
ON DUPLICATE KEY UPDATE image_url = VALUES(image_url);

-- ---------------------------------------------------------------------------
-- 3. 1번 사용자의 초기 상태
-- ---------------------------------------------------------------------------
-- 진단 결과. 홈 화면과 AI 추천이 이 행을 읽는다.
INSERT INTO DIAGNOSIS_RESULT (result_id, user_id, skin_type, result_summary)
VALUES (1, 1, '건성', '건성 진단 결과')
ON DUPLICATE KEY UPDATE skin_type = VALUES(skin_type), result_summary = VALUES(result_summary);

-- 스킨몽. 이 행이 없으면 GET /api/home 이 500 이다.
INSERT INTO SKINMON (skinmon_id, user_id, result_id, skinmon_name, appearance_id)
SELECT 1, 1, 1, '몽이', a.appearance_id
FROM SKINMON_APPEARANCE a
WHERE a.skin_type = '건성' AND a.expression_type = 'happy'
ON DUPLICATE KEY UPDATE skinmon_name = VALUES(skinmon_name);

-- 아침 / 저녁 루틴
INSERT INTO USER_ROUTINE (routine_id, user_id, time_type) VALUES
    (1, 1, 'MORNING'),
    (2, 1, 'EVENING')
ON DUPLICATE KEY UPDATE time_type = VALUES(time_type);

INSERT INTO ROUTINE_ITEM (item_id, routine_id, item_name, description, step_order, is_ai_recommended) VALUES
    (1, 1, '약산성 클렌저', '미온수로 30초',        1, 0),
    (2, 1, '토너',         '건조한 부위 먼저',      2, 0),
    (3, 1, '수분크림',      '세안 후 3분 안에',      3, 0),
    (4, 1, '선크림',        'SPF50+ / PA++++',     4, 0),
    (5, 2, '클렌징 오일',    '선크림부터 정리',       1, 0),
    (6, 2, '세라마이드 세럼', '장벽 케어',            2, 0),
    (7, 2, '수분크림',      '유분감 있는 제품',      3, 0)
ON DUPLICATE KEY UPDATE item_name = VALUES(item_name), description = VALUES(description),
                        step_order = VALUES(step_order);

-- 오늘자 케어메모
INSERT INTO CARE_MEMO (memo_id, user_id, target_date, content, is_completed)
VALUES (1, 1, CURDATE(), '선크림 구매', 0)
ON DUPLICATE KEY UPDATE content = VALUES(content);

-- 알림 설정: UV 09:00 켜짐, 자외선 위험 경보 켜짐
INSERT INTO NOTIFICATION_SETTING (noti_id, user_id, noti_type, is_active)
VALUES (1, 1, 'UV', 1)
ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);

INSERT INTO NOTIFICATION_TIME (noti_id, alert_time)
VALUES (1, '09:00')
ON DUPLICATE KEY UPDATE alert_time = VALUES(alert_time);

INSERT INTO NOTIFICATION_WARNING_SETTING (user_id, is_active)
VALUES (1, 1)
ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);

INSERT INTO NOTIFICATION_LOCATION (user_id, sido, gugun)
VALUES (1, '서울특별시', '강남구')
ON DUPLICATE KEY UPDATE sido = VALUES(sido), gugun = VALUES(gugun);
