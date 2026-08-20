-- 심사용 계정. 제출 양식에 적는 값과 같아야 한다.
--
--   ID: testuser
--   PW: testuser
--
-- 로그인은 phone 또는 email 로 조회하므로, 전화번호가 아닌 로그인 아이디는 email 컬럼에 담는다
-- (AuthService.logIn 의 WHERE phone = ? OR email = ?).
--
-- 적용:
--   mysql -h <host> -P 3306 -u <user> -p likelion < src/main/resources/db/setup/04-review-account.sql
--
-- 여러 번 실행해도 같은 상태가 된다. 심사 중 데이터가 망가지면 다시 실행하면 초기 상태로 돌아온다.

-- 1. 계정. password 는 'testuser' 의 BCrypt 해시다.
INSERT INTO `USER` (email, phone, nickname, region, password)
VALUES ('testuser', NULL, '심사용계정', '서울특별시 강남구',
        '$2a$10$JYnJ915iByhKFR.27evCDulp5RYDIKoVlYLKMNUlnnsQgxmCwuvJu')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), region = VALUES(region),
                        password = VALUES(password);

-- 이후 문장들이 참조할 user_id
SET @uid = (SELECT user_id FROM `USER` WHERE email = 'testuser');

-- 2. 진단 결과. 홈 화면과 AI 추천이 이 행을 읽는다.
INSERT INTO DIAGNOSIS_RESULT (user_id, skin_type, result_summary)
SELECT @uid, '건성', '피부의 유분과 수분이 부족해 세안 후 당김이나 건조함을 쉽게 느낄 수 있어요.'
WHERE NOT EXISTS (SELECT 1 FROM DIAGNOSIS_RESULT WHERE user_id = @uid);

SET @rid = (SELECT result_id FROM DIAGNOSIS_RESULT WHERE user_id = @uid
            ORDER BY diagnosed_at DESC, result_id DESC LIMIT 1);

-- 3. 스킨몽. 없으면 GET /api/home 이 404 다.
INSERT INTO SKINMON (user_id, result_id, skinmon_name, appearance_id)
SELECT @uid, @rid, '몽이', a.appearance_id
FROM SKINMON_APPEARANCE a
WHERE a.skin_type = '건성' AND a.expression_type = 'happy'
ON DUPLICATE KEY UPDATE result_id = VALUES(result_id), skinmon_name = VALUES(skinmon_name),
                        appearance_id = VALUES(appearance_id);

-- 4. 아침 / 저녁 루틴과 항목
INSERT INTO USER_ROUTINE (user_id, time_type) VALUES (@uid, 'MORNING')
ON DUPLICATE KEY UPDATE time_type = VALUES(time_type);
INSERT INTO USER_ROUTINE (user_id, time_type) VALUES (@uid, 'EVENING')
ON DUPLICATE KEY UPDATE time_type = VALUES(time_type);

SET @morning = (SELECT routine_id FROM USER_ROUTINE WHERE user_id = @uid AND time_type = 'MORNING');
SET @evening = (SELECT routine_id FROM USER_ROUTINE WHERE user_id = @uid AND time_type = 'EVENING');

INSERT INTO ROUTINE_ITEM (routine_id, item_name, description, step_order, is_ai_recommended)
SELECT @morning, v.item_name, v.description, v.step_order, 0
FROM (SELECT '약산성 클렌저' AS item_name, '미온수로 30초' AS description, 1 AS step_order
      UNION ALL SELECT '수분 토너', '세안 후 빠른 수분 공급', 2
      UNION ALL SELECT '보습 크림', '건조함 집중 케어', 3
      UNION ALL SELECT '선크림', 'SPF50+ / PA++++', 4) v
WHERE NOT EXISTS (SELECT 1 FROM ROUTINE_ITEM WHERE routine_id = @morning AND deleted_at IS NULL);

INSERT INTO ROUTINE_ITEM (routine_id, item_name, description, step_order, is_ai_recommended)
SELECT @evening, v.item_name, v.description, v.step_order, 0
FROM (SELECT '클렌징 오일' AS item_name, '자외선 차단제 정리' AS description, 1 AS step_order
      UNION ALL SELECT '세라마이드 세럼', '피부 장벽 강화', 2
      UNION ALL SELECT '수분크림', '유분감 있는 제품', 3) v
WHERE NOT EXISTS (SELECT 1 FROM ROUTINE_ITEM WHERE routine_id = @evening AND deleted_at IS NULL);

-- 5. 오늘자 케어메모
INSERT INTO CARE_MEMO (user_id, target_date, content, is_completed)
SELECT @uid, CURDATE(), '선크림 구매하기', 0
WHERE NOT EXISTS (SELECT 1 FROM CARE_MEMO WHERE user_id = @uid AND target_date = CURDATE());

-- 6. 알림 설정: UV 09:00, 자외선 위험 경보 on, 발송 지역
INSERT INTO NOTIFICATION_SETTING (user_id, noti_type, is_active) VALUES (@uid, 'UV', 1)
ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);

SET @noti = (SELECT noti_id FROM NOTIFICATION_SETTING WHERE user_id = @uid AND noti_type = 'UV');

INSERT INTO NOTIFICATION_TIME (noti_id, alert_time) VALUES (@noti, '09:00')
ON DUPLICATE KEY UPDATE alert_time = VALUES(alert_time);

INSERT INTO NOTIFICATION_WARNING_SETTING (user_id, is_active) VALUES (@uid, 1)
ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);

INSERT INTO NOTIFICATION_LOCATION (user_id, sido, gugun) VALUES (@uid, '서울특별시', '강남구')
ON DUPLICATE KEY UPDATE sido = VALUES(sido), gugun = VALUES(gugun);
