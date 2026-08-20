# Rayder Backend API Spec

이 문서는 `src/main/java` 의 컨트롤러/서비스 구현을 그대로 옮긴 것이다. 구현과 다르면 구현이 맞다.

- Base URL: `http://localhost:8080` (`server.port: 8080`, 프로파일 `local` / `prod`)
- 요청·응답 본문은 모두 JSON (`Content-Type: application/json`)
- 시간대는 `Asia/Seoul` 기준

문서 구성상 알림 / 루틴 / AI 추천 섹션은 작성 당시 영문 원문을 그대로 유지했다.

---

## 1. 공통 규약

### 1.1 인증 현황

현재 인증은 **구현되어 있지 않다.** 두 가지 방식이 섞여 있다.

| 계열 | 사용자 식별 방식 | 현재 호출 가능 여부 |
|---|---|---|
| 진단 · 스킨몽 · 홈 · 챗봇 · 로그인 | 클라이언트가 `userId` 를 본문/쿼리로 직접 전달 | 가능 |
| 루틴 · 케어메모 · 알림 · AI 추천 | `@RequestAttribute("authenticatedUserId")` | **불가** |

후자는 요청 속성 `authenticatedUserId` 를 읽지만, 이 값을 세팅하는 필터·인터셉터가 코드에 없다. 따라서 해당 엔드포인트는 호출 시 속성 바인딩 실패로 `500 INTERNAL_SERVER_ERROR` 가 된다. JWT 필터를 붙이는 것이 통합 선행 조건이다.

### 1.2 에러 응답 형식

```json
{
  "timestamp": "2026-08-20T06:53:52.149Z",
  "status": 400,
  "code": "INVALID_REQUEST_BODY",
  "message": "Malformed JSON or unsupported field value",
  "path": "/api/diagnosis/submit"
}
```

`GlobalExceptionHandler` 의 매핑은 다음과 같다.

| 예외 | status | code |
|---|---|---|
| `BusinessException` (및 하위) | 예외가 지정한 status | 예외가 지정한 code |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `HttpMessageNotReadableException` | 400 | `INVALID_REQUEST_BODY` |
| `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException` | 400 | `INVALID_REQUEST_PARAMETER` |
| 그 외 모든 예외 | 500 | `INTERNAL_SERVER_ERROR` (message 는 항상 `Unexpected error occurred`) |

실패한 요청은 `REQUEST` 로그 테이블에도 기록된다.

> 진단 · 스킨몽 · 홈 · 챗봇 서비스는 `IllegalArgumentException` / `IllegalStateException` 을 던지므로 전부 마지막 줄에 걸려 **500** 이 된다. 원인 메시지는 응답에 나오지 않고 서버 로그에만 남는다. 400/404 로 내려야 할 케이스는 아래 각 절에 표시했다.

### 1.3 엔드포인트 인덱스

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/auth/login` | 테스트 계정 조회 | - |
| POST | `/api/diagnosis/submit` | 7문항 진단 저장 및 피부타입 판정 | userId 직접 전달 |
| POST | `/api/skinmon` | 스킨몽 생성(이름 짓기) | userId 직접 전달 |
| GET | `/api/home` | 홈 화면 집계 | userId 직접 전달 |
| POST | `/api/chat` | 규칙 기반 챗봇 | userId 직접 전달 |
| GET | `/api/environment/uv` | 지역명 기준 자외선 지수 | - |
| GET | `/api/environment/dust` | 지역명 기준 미세먼지 | - |
| GET | `/api/environment/uv/by-location` | 좌표 기준 자외선 지수 | - |
| GET | `/api/environment/dust/by-location` | 좌표 기준 미세먼지 | - |
| GET | `/api/location` | 좌표 → 행정구역 역지오코딩 | - |
| GET | `/api/routines` | 날짜별 루틴 조회 | authenticatedUserId |
| POST | `/api/routines` | 루틴 생성 | authenticatedUserId |
| POST | `/api/routines/from-ai` | AI 추천을 내 루틴으로 저장 | authenticatedUserId |
| POST | `/api/routines/{routineId}/items` | 루틴 항목 추가 | authenticatedUserId |
| PATCH | `/api/routine-items/{itemId}` | 항목 수정 | authenticatedUserId |
| DELETE | `/api/routine-items/{itemId}` | 항목 삭제(soft) | authenticatedUserId |
| PUT | `/api/routines/{routineId}/items/order` | 항목 순서 변경 | authenticatedUserId |
| PUT | `/api/routine-items/{itemId}/completion` | 날짜별 완료 처리 | authenticatedUserId |
| POST | `/api/care-memos` | 케어메모 생성 | authenticatedUserId |
| PATCH | `/api/care-memos/{memoId}` | 케어메모 수정 | authenticatedUserId |
| PUT | `/api/care-memos/{memoId}/completion` | 케어메모 완료 처리 | authenticatedUserId |
| DELETE | `/api/care-memos/{memoId}` | 케어메모 삭제 | authenticatedUserId |
| GET | `/api/notifications` | 알림 설정 조회 | authenticatedUserId |
| POST | `/api/notifications` | 알림 설정 생성 | authenticatedUserId |
| PUT | `/api/notifications/{notificationId}` | 알림 설정 교체 | authenticatedUserId |
| DELETE | `/api/notifications/{notificationId}` | 알림 설정 삭제 | authenticatedUserId |
| PUT | `/api/notifications/uv-risk-warning` | 자외선 위험 경보 on/off | authenticatedUserId |
| GET | `/api/notifications/location` | 알림 발송 지역 조회 | authenticatedUserId |
| PUT | `/api/notifications/location` | 알림 발송 지역 변경 | authenticatedUserId |
| POST | `/api/notifications/devices` | 디바이스 토큰 등록 | authenticatedUserId |
| DELETE | `/api/notifications/devices` | 디바이스 토큰 해제 | authenticatedUserId |
| POST | `/api/ai-routines/recommend` | AI 루틴 추천 생성 | authenticatedUserId |

`GET /api/examples`, `GET /api/examples/{id}` 는 초기 스캐폴드 잔재이며 서비스 대상이 아니다.

---

## 2. 인증 API

### 로그인 (테스트용)

`POST /api/auth/login` → `200 OK`

요청 본문 없음. `USER` 테이블의 1번 테스트 계정을 그대로 반환한다.

```json
{
  "userId": 1,
  "email": "test@example.com",
  "password": null,
  "nickname": "테스터",
  "region": "서울특별시 강남구"
}
```

조회 쿼리는 `SELECT user_id, email, nickname, region FROM USER WHERE user_id = 1` 이다. DTO 에 `password` 필드가 남아 있지만 조회하지 않으므로 응답에서는 항상 `null` 이다.

> 실제 인증이 아니다. 자격증명 검증도, 토큰 발급도 하지 않는다. JWT 도입 시 이 엔드포인트는 교체 대상이며 `password` 필드도 DTO 에서 제거해야 한다.

---

## 3. 피부진단 API

### 진단 제출

`POST /api/diagnosis/submit` → `200 OK`

```json
{
  "userId": 1,
  "answers": ["stronglyAgree","neutral","disagree","slightlyAgree","neutral","disagree","neutral"]
}
```

`answers` 는 **정확히 7개**, 아래 문항 순서와 1:1 대응한다.

| # | 문항 |
|---|---|
| 1 | 평소 피부에 유분이 많아 번들거리나요? |
| 2 | 화장품을 바꾸면 피부가 쉽게 예민해지나요? |
| 3 | 자외선에 노출되면 피부가 쉽게 붉어지나요? |
| 4 | 평소 세안 후 피부 당김이 느껴지나요? |
| 5 | T존(이마·코)과 볼의 피부 상태 차이가 뚜렷한가요? |
| 6 | 실내에 있어도 피부가 쉽게 건조해지나요? |
| 7 | 미세먼지가 심한 날 피부가 답답하게 느껴지나요? |

허용 값과 점수: `stronglyAgree`=3, `slightlyAgree`=2, `neutral`=1, `disagree`=0.

응답:

```json
{"resultId": 12, "skinType": "건성피부"}
```

판정 순서 (먼저 만족하는 조건에서 확정):

1. 민감 점수(2번+3번) ≥ 4 → `민감성`
2. 복합 점수(5번) ≥ 2 → `복합성`
3. \|건성 점수(4번+6번) − 지성 점수(1번+7번)\| ≤ 2 → `복합성`
4. 그 외 → 건성 점수가 크면 `건성`, 아니면 `지성`

저장 결과: `DIAGNOSIS_ANSWER` 에 7행, `DIAGNOSIS_RESULT` 에 1행(`result_summary` = `"<타입> 진단 결과"`). 응답의 `skinType` 은 판정값에 `"피부"` 를 붙인 표시용 문자열이고, DB `skin_type` 에는 접미사 없이 저장된다.

오류:

| 상황 | 현재 응답 | 바람직한 응답 |
|---|---|---|
| `answers` 가 null이거나 7개가 아님 | 500 `INTERNAL_SERVER_ERROR` | 400 |
| 허용되지 않는 답변 문자열 | 500 `INTERNAL_SERVER_ERROR` | 400 |

---

## 4. 스킨몽 API

### 스킨몽 생성

`POST /api/skinmon` → `200 OK`

```json
{"userId": 1, "resultId": 12, "skinmonName": "몽이"}
```

```json
{"skinmonId": 5, "skinmonName": "몽이", "skinType": "건성", "expressionType": "happy"}
```

`skinType` 은 `resultId` 로 조회한 `DIAGNOSIS_RESULT.skin_type` 이다. 생성 시 표정은 항상 `happy` 로 고정되며, `SKINMON_APPEARANCE` 에서 `(skin_type, expression_type)` 에 해당하는 `appearance_id` 를 찾아 `SKINMON` 에 저장한다.

오류:

| 상황 | 현재 응답 | 바람직한 응답 |
|---|---|---|
| `resultId` 에 해당하는 진단 결과 없음 | 500 `INTERNAL_SERVER_ERROR` | 404 |
| 해당 피부타입/표정 외형이 `SKINMON_APPEARANCE` 에 없음 | 500 `INTERNAL_SERVER_ERROR` | 500 (데이터 준비 문제) |

---

## 5. 홈 API

### 홈 화면 조회

`GET /api/home?userId=1&areaNo=1168000000` → `200 OK`

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `userId` | int | 사용자 ID |
| `areaNo` | string | 기상청 동네예보 지점코드. `RegionResolver`(`kma-area-codes.csv`)로 구·군명에서 얻는다 |

```json
{
  "uvIndex": 5.0,
  "dustIndex": null,
  "exposureRate": 12.6900,
  "maxUvToday": 7.0,
  "skinType": "건성",
  "expressionType": "happy",
  "hourlyForecast": [
    {"hourOffset": 0, "value": 3.0},
    {"hourOffset": 3, "value": 5.0},
    {"hourOffset": 6, "value": 7.0}
  ]
}
```

계산 규칙:

- `hourlyForecast` 는 기상청 발표시각 기준 0~24시간 후를 3시간 간격으로 담는다. `hourOffset` 은 발표시각 기준 경과 시간이다.
- `maxUvToday` 는 위 구간의 최댓값.
- `exposureRate` = (현재 시각까지 지난 구간의 UV 합 ÷ 63) × 100, 최대 100. 분모 63은 "높음 상한 7 × 3시간 간격 9구간"을 하루치 100%로 본 값이다. 나눗셈에서 소수 4자리로 반올림(HALF_UP)하므로 값은 `12.6900` 처럼 소수 4자리로 내려간다.
- "지난 구간" 판정은 `hourOffset <= 현재 시각의 시(hour)` 로 계산한다. 발표시각 기준 오프셋과 벽시계 시를 비교하는 방식이라 발표시각이 자정 부근이면 실제 경과와 어긋날 수 있다.
- `uvIndex` 는 지난 구간 중 마지막 값.
- `expressionType` 은 `exposureRate` ≥ 80 이면 `sad`, 아니면 `happy`.
- 조회 시 `DAILY_UV_STATUS` 를 오늘 날짜로 upsert 한다(`uv_index`, `exposure_rate`, `max_uv_today`).

> `dustIndex` 는 `DAILY_UV_STATUS.dust_index` 를 그대로 읽어 내리는데, 이 컬럼을 쓰는 코드가 없어 현재는 **항상 null** 이다. 미세먼지 값이 필요하면 `/api/environment/dust` 를 별도로 호출하거나 upsert 에 컬럼을 추가해야 한다.

오류:

| 상황 | 현재 응답 | 바람직한 응답 |
|---|---|---|
| 해당 사용자의 스킨몽이 없음 | 500 `INTERNAL_SERVER_ERROR` | 404 |
| 기상청 API 실패 | 502 `ENVIRONMENT_UPSTREAM_ERROR` | 동일 |

---

## 6. 챗봇 API

### 메시지 전송

`POST /api/chat` → `200 OK`

```json
{"userId": 1, "message": "선크림 뭐 써야 해?"}
```

```json
{"reply": "자외선 지수가 높은 날에는 SPF50+ / PA++++ 제품을 권해요.\n..."}
```

LLM 을 쓰지 않는 **규칙 기반 응답**이다. 키워드 매칭 순서대로 첫 번째로 걸린 답변을 반환한다.

| 순서 | 키워드 | 답변 주제 |
|---|---|---|
| 1 | 선크림, 자외선, uv, UV, spf, SPF | 자차 지수별 SPF/PA 권장 |
| 2 | 세안, 클렌징, 씻 | 세안 방법 |
| 3 | 순서, 루틴 | 아침·저녁 루틴 순서 |
| 4 | 추천, 제품, 뭐 바, 사야 | 제품 추천 |
| 5 | 따갑, 열, 붉, 빨개, 가렵 | 자극 진정 |
| - | 그 외 | 기본 안내 문구 |

대화방은 `CHATBOT_CONVERSATION` 에서 해당 사용자의 가장 최근 방(`ORDER BY started_at DESC LIMIT 1`)을 재사용하고, 없으면 새로 만든다. 요청 메시지는 `sender_type='USER'`, 응답은 `'BOT'` 으로 `CHATBOT_MESSAGE` 에 저장된다.

---

## 7. 환경 정보 API

응답 공통 형식(`EnvironmentInfo`):

```json
{
  "type": "UV",
  "value": 6.0,
  "level": "높음",
  "region": "서울특별시 강남구",
  "observedAt": "2026-08-20T15:00:00"
}
```

`type` 은 `UV`, `DUST_PM10`, `DUST_PM25` 중 하나다.

자외선 등급: 3 미만 `낮음`, 5 이하 `보통`, 7 이하 `높음`, 10 이하 `매우높음`, 초과 `위험`.
미세먼지 등급: 에어코리아 grade 값(`1`~`4`)을 `좋음`/`보통`/`나쁨`/`매우나쁨` 으로 매핑하고, grade 가 없으면 농도 임계값으로 판정한다.

| Method | Path | 파라미터 | 응답 |
|---|---|---|---|
| GET | `/api/environment/uv` | `sido`, `gugun` | `EnvironmentInfo` |
| GET | `/api/environment/dust` | `sido`, `gugun` | `EnvironmentInfo` 배열 (PM10, PM2.5 순서) |
| GET | `/api/environment/uv/by-location` | `lat`, `lon` | `EnvironmentInfo` |
| GET | `/api/environment/dust/by-location` | `lat`, `lon` | `EnvironmentInfo` 배열 |
| GET | `/api/location` | `lat`, `lon` | `{"sido":"서울특별시","gugun":"강남구","dong":"역삼동"}` |

오류:

| status | code | 상황 |
|---|---|---|
| 400 | `REGION_NOT_FOUND` | `kma-area-codes.csv` 에 없는 지역명 |
| 400 | `INVALID_ENVIRONMENT_REQUEST` | 좌표 범위 등 입력 오류 |
| 400 | `INVALID_REQUEST_PARAMETER` | 필수 파라미터 누락/타입 불일치 |
| 502 | `ENVIRONMENT_UPSTREAM_ERROR` | 기상청·에어코리아·카카오 호출 실패 또는 오류 응답 |

필요 키: `DATA_GO_KR_SERVICE_KEY`(기상청·에어코리아), `KAKAO_REST_API_KEY`(역지오코딩).

---

# Notification API

All endpoints require authentication. The JWT authentication filter stores the token's user ID as a `Long` request attribute named `authenticatedUserId`.

Errors use the common body: `timestamp`, `status`, `code`, `message`, and `path`.

## List settings

`GET /api/notifications` → `200 OK`

```json
{
  "notifications": [
    {
      "notificationId": 1,
      "type": "UV",
      "enabled": true,
      "times": ["09:00"],
      "createdAt": "2026-08-19T10:00:00",
      "updatedAt": "2026-08-19T10:00:00"
    },
    {
      "notificationId": null,
      "type": "DUST",
      "enabled": false,
      "times": [],
      "createdAt": null,
      "updatedAt": null
    },
    {
      "notificationId": null,
      "type": "ROUTINE",
      "enabled": false,
      "times": [],
      "createdAt": null,
      "updatedAt": null
    }
  ],
  "uvRiskWarning": {
    "enabled": true,
    "createdAt": "2026-08-19T10:00:00",
    "updatedAt": "2026-08-19T10:00:00"
  }
}
```

`notifications` always contains `UV`, `DUST`, and `ROUTINE` in that order. A type without a database row is returned as `notificationId=null`, `enabled=false`, `times=[]`, and null timestamps.

## Create scheduled setting

`POST /api/notifications` → `201 Created`

```json
{"type":"ROUTINE","enabled":true,"times":["08:00","21:00"]}
```

Returns the created notification object. A duplicate user/type returns `409 NOTIFICATION_ALREADY_EXISTS`.

## Replace scheduled setting

`PUT /api/notifications/{notificationId}` → `200 OK`

```json
{"enabled":false,"times":["09:00"]}
```

The complete time list is replaced atomically. At least one time is required when `enabled=true`; `enabled=false` permits an empty list. Missing enabled times return `400 NOTIFICATION_TIME_REQUIRED`, duplicate times return `400 DUPLICATE_NOTIFICATION_TIME`, and invalid `HH:mm` values return `400 INVALID_NOTIFICATION_TIME`.

## Delete scheduled setting

`DELETE /api/notifications/{notificationId}` → `204 No Content`

Missing settings return `404 NOTIFICATION_NOT_FOUND`; access by another user returns `403 NOTIFICATION_FORBIDDEN`.

## Update UV-risk warning

`PUT /api/notifications/uv-risk-warning` → `200 OK`

```json
{"enabled":true}
```

## Notification delivery location

The service stores one explicitly selected current notification region per user. It does not retain location history or infer outdoor activity.

- `GET /api/notifications/location` -> `200 OK` with `{"sido":"서울특별시","gugun":"강남구"}`, or an empty body when unset.
- `PUT /api/notifications/location` -> `200 OK` with the normalized region. Body: `{"sido":"서울특별시","gugun":"강남구"}`.

The same region is used for scheduled UV, scheduled dust, and UV-risk warning delivery. ROUTINE delivery does not require a region.

## Device tokens

- `POST /api/notifications/devices` -> `204 No Content`. Body: `{"token":"ExpoPushToken[...]","platform":"ANDROID"}`.
- `DELETE /api/notifications/devices` -> `204 No Content`. Body: `{"token":"ExpoPushToken[...]"}`.

Platforms are `ANDROID`, `IOS`, and `WEB`. Registration is an upsert and reactivates a previously disabled token. Unregistration soft-disables the token.

## Delivery policy

- All schedules use `Asia/Seoul`.
- UV sends the selected region's UV index and level at each configured time.
- DUST sends PM10 and PM2.5 values and levels at each configured time.
- ROUTINE sends a routine reminder at each configured time.
- UV-risk warning checks KMA current/near-term forecast values and sends when the UV index is at least 6 (`높음`, `매우높음`, or `위험`). Users configure only the warning's enabled flag.
- The backend stores a `(user_id, forecast_at)` delivery marker to prevent repeated delivery of the same UV forecast. It stores no personal UV dose or location history.

Expo Push delivery requires `EXPO_PUSH_ENABLED=true`. If enhanced push security is enabled in EAS, set `EXPO_ACCESS_TOKEN`; otherwise it may be empty. The backend sends tickets through Expo Push Service and checks receipts after at least 15 minutes. `DeviceNotRegistered` tokens are disabled automatically.

# Routine API

All routine endpoints use the authenticated `authenticatedUserId`; clients never send a user ID.

## Read routines for a date

`GET /api/routines?date=2026-08-19` → `200 OK`

```json
{
  "date": "2026-08-19",
  "morning": {
    "routineId": 10,
    "type": "MORNING",
    "items": [{"id": 100,"name":"세안","detail":"미온수","done":true,"order":1}]
  },
  "evening": {"routineId": 11,"type":"EVENING","items":[]},
  "memos": [{"id":20,"date":"2026-08-19","content":"선크림 구매","done":false}],
  "completedCount": 1,
  "totalCount": 1,
  "progressRate": 100
}
```

The completion join, counts, and rounded integer percentage are calculated by the backend. A missing morning or evening routine is returned with a `null` `routineId` and an empty item list. Invalid dates return `400 INVALID_DATE`.

## Create a routine

`POST /api/routines` → `201 Created`

```json
{"type":"MORNING"}
```

Only `MORNING` and `EVENING` are accepted. One routine per user/type is allowed; duplicates return `409 ROUTINE_ALREADY_EXISTS`.

## Manage items

- `POST /api/routines/{routineId}/items` with `{"name":"세안","detail":"미온수"}` → `201 Created`
- `PATCH /api/routine-items/{itemId}` with the same fields → `200 OK`
- `DELETE /api/routine-items/{itemId}` → `204 No Content` (soft delete)
- `PUT /api/routines/{routineId}/items/order` with `{"itemIds":[103,101,102]}` → `200 OK`

An order request must contain every active item exactly once. Deleted items disappear from current queries but remain in the database.

## Set date-specific completion

`PUT /api/routine-items/{itemId}/completion` → `200 OK`

```json
{"date":"2026-08-19","completed":true}
```

The same endpoint sets `completed` back to `false`. `(item_id, completion_date)` is unique, so the operation updates rather than duplicates a record.

## Care memos

- Memos for the requested date are included in `GET /api/routines`.
- `POST /api/care-memos` with `{"date":"2026-08-19","content":"선크림 구매"}` → `201 Created`
- `PATCH /api/care-memos/{memoId}` with `{"content":"립밤 구매"}` → `200 OK`
- `PUT /api/care-memos/{memoId}/completion` with `{"completed":true}` → `200 OK`
- `DELETE /api/care-memos/{memoId}` → `204 No Content`

Cross-user routine, item, completion, and memo operations return `403`; missing resources return `404`.

# AI routine recommendation API (implemented; live integration not yet verified)

All endpoints derive the user from authenticated `authenticatedUserId` (JWT integration boundary). Clients never send `userId`. Generated recommendations have no ID and are not persisted.

## Generate recommendation

`POST /api/ai-routines/recommend` -> `200 OK`

```json
{"latitude":37.5172,"longitude":127.0473}
```

Coordinates are required request-scoped inputs for `EnvironmentQueryService`, not user-profile data. The service reads the authenticated user's latest `DIAGNOSIS_RESULT`, then combines it with UV, PM10, PM2.5, and later RAG context. It does not persist its output.

```json
{
  "skinType": "DRY",
  "diagnosisResult": "...",
  "environment": {
    "available": true,
    "uvLevel": "높음",
    "dustLevel": "좋음 / 좋음"
  },
  "reasons": ["수분 보충과 장벽 관리가 필요해요."],
  "morning": [{"order":1,"name":"약산성 클렌저","detail":"쌓인 노폐물 제거"}],
  "evening": [{"order":1,"name":"클렌징 오일","detail":"자외선 차단제 제거"}]
}
```

`skinType` is the `VARCHAR(20)` value from `DIAGNOSIS_RESULT.skin_type`; `diagnosisResult` is the nullable `VARCHAR(255)` value from `result_summary`. Since the table has no completion-status column, the latest row is selected by `user_id` with `ORDER BY diagnosed_at DESC, result_id DESC LIMIT 1`. Invalid/missing coordinates return `400`; no diagnosis returns `404 DIAGNOSIS_RESULT_NOT_FOUND`. If environment lookup fails, generation falls back to diagnosis plus RAG and returns `environment.available=false` without invented values.

Response validation requires 1–5 morning items, 1–5 evening items, and at most 3 reasons. Names are 1–20 characters, details are 1–30 characters, and each section's order is consecutive from 1.

## Read recommendation

There is no recommendation read endpoint. Retrying generation creates a new transient response.

## Save displayed recommendation as user routine

`POST /api/routines/from-ai` -> `201 Created`

```json
{
  "morning": [{"order":1,"name":"약산성 클렌저","detail":"쌓인 노폐물 제거"}],
  "evening": [{"order":1,"name":"클렌징 오일","detail":"자외선 차단제 제거"}]
}
```

The request contains only the recommendation items currently displayed and selected for conversion. It contains no recommendation ID, reasons, diagnosis/environment data, or user ID. Both arrays cannot be empty simultaneously. Each has at most five items; names are required and at most 20 characters, details are required and at most 30 characters, and order is consecutive from 1.

This endpoint does not persist an AI recommendation and never calls OpenAI. In one transaction it converts the submitted items into ordinary user routine data: reuse or create each authenticated user's `USER_ROUTINE`, append to `ROUTINE_ITEM`, and skip exact-name duplicates in the same time section and request. Existing routine entries remain unchanged. No completion row is created; later checks use the existing date-specific completion endpoint.

---

## 8. 알려진 통합 이슈

1. **인증 미구현.** `authenticatedUserId` 를 세팅하는 필터가 없어 루틴 · 케어메모 · 알림 · AI 추천(22개 엔드포인트)은 현재 호출되지 않는다.
2. **사용자 식별 방식 이원화.** 진단 · 스킨몽 · 홈 · 챗봇은 클라이언트가 `userId` 를 보낸다. 인증 도입 시 이 파라미터는 제거 대상이다.
3. **에러 시맨틱.** 진단 · 스킨몽 · 홈 계열의 검증 실패가 모두 500 으로 나간다. `BusinessException` 을 상속한 예외로 바꿔야 400/404 로 구분된다.
4. **홈 응답의 `dustIndex` 가 항상 null.** 7절의 미세먼지 API 와 연결되어 있지 않다.
5. **`POST /api/auth/login` 이 평문 비밀번호를 반환한다.**
