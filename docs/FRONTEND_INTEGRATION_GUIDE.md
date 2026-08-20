# Frontend integration guide

This guide describes the current backend code, not proposed endpoints. Backend base URL is `http://localhost:8080` by default. Prefer a frontend environment variable such as `VITE_API_BASE_URL` instead of embedding it in components.

## 로컬 연동 준비 (2026-08-21 갱신)

### DB

`src/main/resources/db/setup/` 에 세 개가 있다. 상황에 맞는 것만 쓴다.

| 파일 | 용도 |
|---|---|
| `01-schema.sql` | 빈 DB 를 처음부터 만들 때. 모든 테이블을 최종 형태로 생성 |
| `02-seed-dev.sql` | **빈 로컬 DB 전용** 시드. 테스트 계정·진단 결과·스킨몽·루틴·메모·알림 |
| `03-server-delta.sql` | 이미 돌아가는 공용 서버 DB 에 회원가입/로그인을 적용하는 델타 |
| `04-review-account.sql` | 심사용 계정(`testuser` / `testuser`)과 시연용 초기 데이터 |

공용 서버 DB(기본 스키마 `likelion`)는 대부분 이미 들어가 있다. 부족한 것만 델타로 적용한다.

```bash
mysql -h <db-host> -P 3306 -u <db-user> -p likelion \
  < src/main/resources/db/setup/03-server-delta.sql
```

델타가 하는 일은 세 가지다. `USER` 에 `phone` 유니크 컬럼 추가, `email` 을 nullable 로 완화(회원가입이 이메일을 받지 않는다), 1번 테스트 계정의 평문 비밀번호를 BCrypt 해시로 교체(`01000000000` / `P@ssw0rd`). 요청 로그용 `request` 테이블도 없어서 함께 만든다.

> `02-seed-dev.sql` 은 공용 서버에 쓰지 말 것. 빈 DB 기준이라 기존 `DIAGNOSIS_RESULT` 1번 행을 덮어써 스킨몽 외형과 어긋난다.

### 심사용 계정

제출 양식에 적는 값이다. `04-review-account.sql` 이 이 계정과 시연용 데이터(진단 결과, 스킨몽, 아침·저녁 루틴 7개 항목, 케어메모, UV 알림 09:00, 자외선 경보)를 함께 만든다.

```
ID: testuser
PW: testuser
```

로그인 식별자는 휴대폰 번호 또는 이메일 컬럼으로 조회하므로, 전화번호가 아닌 로그인 아이디는 `email` 컬럼에 담았다. 심사 중 데이터가 망가지면 같은 스크립트를 다시 실행하면 초기 상태로 돌아온다.

**심사 기간에는 배포 환경에서 `AUTH_DEV_ENABLED=true` 가 필요하다.** 이 값이 꺼져 있으면 루틴 · 케어메모 · 알림 · AI 화면이 `authenticatedUserId` 를 받지 못해 실패한다. 토큰을 검증하지 않는 임시 브릿지이므로 심사가 끝나면 다시 끄고 JWT 로 교체할 것.

### 백엔드 실행

```bash
export DB_URL='jdbc:mariadb://<db-host>:3306/likelion?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul'
export DB_USERNAME=<db-user> DB_PASSWORD=<db-password>
export DATA_GO_KR_SERVICE_KEY=... KAKAO_REST_API_KEY=...
export OPENAI_API_KEY=...            # AI 추천을 쓸 때만
./gradlew bootRun
```

공공 API 키가 없으면 기동 시점에 실패한다(의도된 동작). 자격증명은 저장소에 넣지 말고 셸이나 배포 환경의 시크릿으로만 주입한다.

운영 프로파일(`SPRING_PROFILES_ACTIVE=prod`)은 위 `DB_*` 를 그대로 쓰고, 프론트를 다른 도메인에서 서빙하면 `WEB_CORS_ALLOWED_ORIGINS` 도 필요하다.

### 프론트엔드 실행

`frontend/.env.example` 을 `.env.local` 로 복사한다. 기본값은 `http://localhost:8080`.

CORS 는 `web.cors.allowed-origins` 로 허용한다. 로컬 프로파일에 Vite dev(5173)·preview(4173)가 등록돼 있고, 다른 포트를 쓰면 `application-local.yml` 에 추가해야 한다.

## 인증: 임시 개발용 브릿지

알림 · 루틴 · 케어메모 · AI 엔드포인트는 요청 속성 `authenticatedUserId` 를 읽는데, 이를 채우는 JWT 필터가 아직 없어서 호출 자체가 불가능했다. 연동을 막지 않기 위해 임시 브릿지(`DevAuthenticationFilter`)를 넣었다.

- `auth.dev.enabled=true` 일 때만 등록되고, 로컬 프로파일에서만 켜져 있다. 운영에서는 절대 켜지 않는다.
- **토큰을 검증하지 않는다.** 사용자는 `Authorization: Bearer dev.<userId>....` 의 userId → `X-Dev-User-Id` 헤더 → `auth.dev.default-user-id`(기본 1) 순서로 정한다.
- 프론트는 `POST /api/auth/login` 응답의 `userId` 로 `dev.<userId>.<timestamp>` 토큰을 만들어 저장하고, 모든 요청에 `Authorization: Bearer` 로 실어 보낸다.

실제 JWT 필터가 들어오면 이 필터는 삭제하고 프론트의 토큰 생성부만 교체하면 된다. 헤더 전송 방식은 그대로 유지된다.

`POST /api/auth/login` 자체도 아직 인증이 아니다. 자격증명을 검증하지 않고 1번 테스트 계정을 그대로 반환하므로, 프론트의 회원가입은 계정을 로컬에 저장한 뒤 이 엔드포인트로 세션을 받는다.

진단 · 스킨몽 · 홈 · 챗봇은 여전히 `userId` 를 쿼리/본문으로 받는다. 인증이 붙으면 이 파라미터는 제거 대상이고, 프론트의 `src/api/userContext.ts` 도 같이 삭제하면 된다.

## Shared contracts

- Dates: `yyyy-MM-dd` (example `2026-08-20`).
- Notification times: strict 24-hour `HH:mm` with leading zeros (example `09:05`).
- Coordinates: `latitude`/`lat` is -90..90; `longitude`/`lon` is -180..180.
- Routine enum: `MORNING`, `EVENING`.
- Notification enum: `UV`, `DUST`, `ROUTINE`. The UV-risk warning is a separate boolean, not another enum value.
- Environment enum: `UV`, `DUST_PM10`, `DUST_PM25`.
- Java `LocalDateTime` values serialize as ISO local timestamps without a guaranteed timezone offset.
- The frontend never sends `userId` for user-specific endpoints.

Common error response:

```json
{
  "timestamp": "2026-08-20T00:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "field: validation message",
  "path": "/api/..."
}
```

Malformed JSON uses `INVALID_REQUEST_BODY`; missing/invalid query parameters use `INVALID_REQUEST_PARAMETER`; unexpected failures use `INTERNAL_SERVER_ERROR`. UI code should branch on `code`, not parse English `message` text.

## Environment and home screen

These five endpoints are implemented and unauthenticated, but live provider verification was not run in this audit.

```http
GET /api/location?lat=37.5172&lon=127.0473
GET /api/environment/uv?sido=서울특별시&gugun=강남구
GET /api/environment/dust?sido=서울특별시&gugun=강남구
GET /api/environment/uv/by-location?lat=37.5172&lon=127.0473
GET /api/environment/dust/by-location?lat=37.5172&lon=127.0473
```

Location response:

```json
{"sido":"서울특별시","gugun":"강남구","dong":"역삼동"}
```

Environment response item:

```json
{
  "type": "UV",
  "value": 5.0,
  "level": "보통",
  "region": "서울특별시 강남구",
  "observedAt": "2026-08-20T12:00:00"
}
```

Dust endpoints return an array containing PM10 and PM2.5 items. Direct environment errors do not fallback; show a retry/unavailable state for `INVALID_ENVIRONMENT_REQUEST`, `REGION_NOT_FOUND`, or `ENVIRONMENT_UPSTREAM_ERROR`.

## AI recommendation screen

Call after the user grants geolocation and after JWT integration:

```http
POST /api/ai-routines/recommend
Content-Type: application/json
Authorization: Bearer <JWT>

{"latitude":37.5172,"longitude":127.0473}
```

Actual response DTO:

```json
{
  "skinType": "건성",
  "diagnosisResult": "세안 후 당김과 볼 부위 건조함",
  "environment": {
    "available": true,
    "uvLevel": "높음",
    "dustLevel": "좋음 / 보통"
  },
  "morning": [{"order":1,"name":"약산성 클렌저","detail":"부드럽게 세안"}],
  "evening": [{"order":1,"name":"보습 크림","detail":"얇게 덧바르기"}],
  "reasons": ["수분과 장벽 관리가 필요해요."]
}
```

When any environment provider fails, the recommendation may still return 200 with:

```json
{"available":false,"uvLevel":null,"dustLevel":null}
```

Render the routine normally, hide environment grades, and show “환경 정보를 불러오지 못했어요” rather than treating this as total request failure. A missing diagnosis is a real 404 `DIAGNOSIS_RESULT_NOT_FOUND`; RAG/OpenAI failures are generally 502, and an absent key is 503 `OPENAI_NOT_CONFIGURED`.

The recommendation is transient. On “체크리스트 저장하기”, send the displayed/selected items:

```http
POST /api/routines/from-ai

{
  "morning": [{"order":1,"name":"약산성 클렌저","detail":"부드럽게 세안"}],
  "evening": [{"order":1,"name":"보습 크림","detail":"얇게 덧바르기"}]
}
```

Each array allows 0..5 items, but both cannot be empty. Orders must be consecutive from 1. Names/details are limited to 20/30 characters. Saving appends non-duplicate items; it does not preserve reasons or environment data.

## My routine screen

On screen entry and whenever the selected date changes:

```http
GET /api/routines?date=2026-08-20
```

```json
{
  "date":"2026-08-20",
  "morning":{"routineId":10,"type":"MORNING","items":[{"id":100,"name":"세안","detail":"미온수","done":true,"order":1}]},
  "evening":{"routineId":11,"type":"EVENING","items":[]},
  "memos":[{"id":20,"date":"2026-08-20","content":"선크림 구매","done":false,"createdAt":"...","updatedAt":"..."}],
  "completedCount":1,
  "totalCount":1,
  "progressRate":100
}
```

If `routineId` is null, create the group before adding an item:

```http
POST /api/routines                 {"type":"MORNING"}
POST /api/routines/{routineId}/items {"name":"세안","detail":"미온수"}
PATCH /api/routine-items/{itemId}    {"name":"세안","detail":"미온수로 부드럽게"}
DELETE /api/routine-items/{itemId}
PUT /api/routines/{routineId}/items/order {"itemIds":[103,101,102]}
PUT /api/routine-items/{itemId}/completion {"date":"2026-08-20","completed":true}
```

Reordering must include every active item ID exactly once. Completion is date-specific; item edits/deletion are not.

Care memo calls:

```http
POST /api/care-memos                       {"date":"2026-08-20","content":"선크림 구매"}
PATCH /api/care-memos/{memoId}             {"content":"립밤 구매"}
PUT /api/care-memos/{memoId}/completion    {"completed":true}
DELETE /api/care-memos/{memoId}
```

## Notification screen

On entry:

```http
GET /api/notifications
```

```json
{
  "notifications":[
    {"notificationId":1,"type":"UV","enabled":true,"times":["09:00"],"createdAt":"...","updatedAt":"..."},
    {"notificationId":null,"type":"DUST","enabled":false,"times":[],"createdAt":null,"updatedAt":null},
    {"notificationId":null,"type":"ROUTINE","enabled":false,"times":[],"createdAt":null,"updatedAt":null}
  ],
  "uvRiskWarning":{"enabled":false,"createdAt":null,"updatedAt":null}
}
```

- The response always contains `UV`, `DUST`, and `ROUTINE`; `notificationId=null` means that type has no database row yet.
- Create a missing scheduled type: `POST /api/notifications` with `{"type":"UV","enabled":true,"times":["09:00"]}`.
- Save an existing type: `PUT /api/notifications/{notificationId}` with the entire replacement `{"enabled":false,"times":[]}`.
- Enabled settings require at least one `HH:mm` time; disabled settings may use an empty list.
- Delete: `DELETE /api/notifications/{notificationId}`.
- Save UV-risk warning: `PUT /api/notifications/uv-risk-warning` with `{"enabled":true}`.

There are no add-time/remove-time endpoints; edit the local list and submit the whole list. Actual push delivery/scheduling is not implemented, so this screen currently manages preferences only.

## 프론트엔드 연동 현황 (2026-08-21)

프론트엔드(`Rayder_frontend`)에 실제 호출이 붙었다. 공용 HTTP 클라이언트는 `src/api/client.ts` 이고, 실패는 백엔드 `code` 를 담은 `ApiError` 로 올라온다.

| 화면 / 모듈 | 상태 | 사용하는 엔드포인트 |
|---|---|---|
| `src/api/client.ts` | 연동 완료 | 공용. `VITE_API_BASE_URL`, Bearer 헤더 자동 첨부 |
| `src/api/authApi.ts` | 연동 완료 | `POST /api/auth/signup`, `POST /api/auth/login`. BCrypt 검증. 토큰은 아직 없음 |
| `src/screens/DiagnosisQuizScreen.tsx` | 연동 완료 | `POST /api/diagnosis/submit` |
| `src/screens/DiagnosisResultScreen.tsx` | 연동 완료 | 피부타입·키워드·설명 모두 진단 응답값 |
| 스킨몽 이름짓기 | 연동 완료 | `POST /api/skinmon` |
| `src/screens/HomeScreen.tsx` | 연동 완료 | `GET /api/home` + `GET /api/environment/dust` |
| `src/screens/ChatScreen.tsx` | 연동 완료 | `POST /api/chat` |
| `src/screens/MyRoutineScreen.tsx` | 연동 완료 | `GET /api/routines`, 항목 생성·수정·삭제, `PUT .../completion`, `POST /api/care-memos` |
| `src/screens/RoutineRecommendationScreen.tsx` | 연동 완료 | `POST /api/ai-routines/recommend`, `POST /api/routines/from-ai` |
| `src/screens/NotificationScreen.tsx` | 연동 완료 | `GET/POST/PUT/DELETE /api/notifications`, `PUT /api/notifications/uv-risk-warning` |
| `src/data/*.ts` | 폴백으로 남김 | 첫 응답 전이나 호출 실패 시 화면이 비지 않도록 유지 |

남은 것

- **토큰이 없다.** 회원가입·로그인은 비밀번호를 검증하지만 JWT 를 발급하지 않는다. 프론트는 응답의 `userId` 로 개발용 토큰을 만들고 서버는 `DevAuthenticationFilter` 로 읽는다. 배포 전 교체 필요.
- 지역은 아직 고정값(`서울특별시 강남구`)이다. 위치 권한을 받으면 `GET /api/location` 으로 대체할 수 있다.
- AI 추천은 백엔드에 `OPENAI_API_KEY` 가 있어야 동작한다.
- 알림 발송 지역(`GET/PUT /api/notifications/location`)과 디바이스 토큰 등록(`/api/notifications/devices`)에 대응하는 화면이 아직 없다.
- 홈 화면 캐릭터, 나의 루틴 캐릭터는 디자인대로 자리만 잡혀 있다(실제 아트 미적용).
- 네비게이션 드로어가 디자인되지 않아 홈 헤더의 햄버거는 임시 목록(나의 루틴 / 알림설정)을 띄운다.
