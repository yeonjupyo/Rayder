# Frontend integration guide

This guide describes the current backend code, not proposed endpoints. Backend base URL is `http://localhost:8080` by default. Prefer a frontend environment variable such as `VITE_API_BASE_URL` instead of embedding it in components.

## 로컬 연동 준비 (2026-08-21 갱신)

1. **DB 생성.** USER · 진단 · 스킨몽 · 홈 · 챗봇 테이블은 어느 마이그레이션에도 생성문이 없었다. 전체 스키마와 시드를 `src/main/resources/db/setup/` 에 넣어뒀다.

   ```bash
   mysql -u root -p -e "CREATE DATABASE hackathon DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci"
   mysql -u root -p hackathon < src/main/resources/db/setup/01-schema.sql
   mysql -u root -p hackathon < src/main/resources/db/setup/02-seed-dev.sql
   ```

   시드는 1번 테스트 사용자, 스킨몽 외형 참조 데이터(없으면 `POST /api/skinmon` 이 항상 실패한다), 진단 결과 1건, 스킨몽 1마리, 아침·저녁 루틴, 케어메모, 알림 설정을 만든다. 이미 돌아가는 DB 가 있으면 `mysqldump --no-data` 로 뽑아 `01-schema.sql` 과 대조할 것.

2. **백엔드 실행.** 공공 API 키는 필수다(없으면 기동 시점에 실패한다).

   ```bash
   export DATA_GO_KR_SERVICE_KEY=... KAKAO_REST_API_KEY=...
   ./gradlew bootRun
   ```

3. **프론트엔드 실행.** `frontend/.env.example` 을 `.env.local` 로 복사한다. 기본값은 `http://localhost:8080`.

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
| `src/api/authApi.ts` | 연동 완료(백엔드가 임시) | `POST /api/auth/login`. 자격증명 검증 없음, 1번 계정 반환. 회원가입은 로컬 저장 후 로그인 |
| `src/screens/DiagnosisQuizScreen.tsx` | 연동 완료 | `POST /api/diagnosis/submit` |
| `src/screens/DiagnosisResultScreen.tsx` | 부분 연동 | 피부타입은 실제 값. 키워드·설명 카피는 여전히 `src/data/diagnosisResult.ts` |
| 스킨몽 이름짓기 | 연동 완료 | `POST /api/skinmon` |
| `src/screens/HomeScreen.tsx` | 연동 완료 | `GET /api/home` + `GET /api/environment/dust` |
| `src/screens/ChatScreen.tsx` | 연동 완료 | `POST /api/chat` |
| `src/screens/MyRoutineScreen.tsx` | 연동 완료 | `GET /api/routines`, 항목 생성·수정·삭제, `PUT .../completion`, `POST /api/care-memos` |
| `src/screens/RoutineRecommendationScreen.tsx` | 연동 완료 | `POST /api/ai-routines/recommend`, `POST /api/routines/from-ai` |
| 알림 화면 | 미구현 | 화면 자체가 없다. `GET/POST/PUT/DELETE /api/notifications` |
| `src/data/*.ts` | 폴백으로 남김 | 첫 응답 전이나 호출 실패 시 화면이 비지 않도록 유지 |

남은 것

- 회원가입 엔드포인트가 없다. 프론트는 계정을 로컬에 저장한 뒤 테스트 계정으로 로그인한다.
- 알림 설정 화면이 없다. 백엔드는 준비돼 있다.
- 진단 결과 화면의 키워드·설명 카피는 백엔드에 대응 필드가 없다. `DIAGNOSIS_RESULT.result_summary` 를 쓸지 결정이 필요하다.
- AI 추천은 백엔드에 `OPENAI_API_KEY` 가 있어야 동작한다.
- 지역은 아직 고정값(`서울특별시 강남구`)이다. 위치 권한을 받으면 `GET /api/location` 으로 대체할 수 있다.
