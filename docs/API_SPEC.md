# Rayder Backend API Spec

이 문서는 `src/main/java` 의 컨트롤러/서비스 구현을 그대로 옮긴 것이다. 구현과 다르면 구현이 맞다.

- Base URL: `http://localhost:8080` (`server.port: 8080`, 프로파일 `local` / `prod`)
- 요청·응답 본문은 모두 JSON (`Content-Type: application/json`)
- 시간대는 `Asia/Seoul` 기준

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

요청 본문은 Bean Validation 으로 먼저 걸러진다(`VALIDATION_ERROR`, 메시지는 `필드: 사유` 형태). 그 다음 도메인 검증은 `BusinessException` 이라 상태·코드가 케이스별로 다르다. `INTERNAL_SERVER_ERROR` 는 예상하지 못한 실패에만 남는다.

### 1.3 엔드포인트 인덱스

| Method | Path | 설명 | 인증 |
|---|---|---|---|
| POST | `/api/auth/signup` | 회원가입 | - |
| POST | `/api/auth/login` | 로그인 | - |
| POST | `/api/diagnosis/submit` | 7문항 진단 저장 및 피부타입 판정 | userId 직접 전달 |
| POST | `/api/skinmon` | 스킨몽 생성·갱신(이름 짓기) | userId 직접 전달 |
| GET | `/api/skinmon/{userId}` | 스킨몽 조회 | userId 직접 전달 |
| PATCH | `/api/skinmon/{skinmonId}/expression` | 표정 변경 | - |
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

로그인 식별자는 **휴대폰 번호(숫자만)** 이고, 비밀번호는 BCrypt 해시로만 저장한다. 시드 계정은 이메일로도 로그인된다. 토큰은 아직 발급하지 않는다(1.1 참고).

### 회원가입

`POST /api/auth/signup` → `201 Created`

```json
{"name": "테스터", "phone": "01012345678", "password": "P@ssw0rd"}
```

| 필드 | 제약 |
|---|---|
| `name` | 2~50자 |
| `phone` | `0` 으로 시작하는 숫자 10~11자리 (하이픈 제거해서 전송) |
| `password` | 8~72자 |

응답은 아래 로그인과 같은 형태다. 오류:

| status | code | 상황 |
|---|---|---|
| 400 | `VALIDATION_ERROR` | 형식 위반 |
| 409 | `DUPLICATE_PHONE` | 이미 가입된 번호 |

### 로그인

`POST /api/auth/login` → `200 OK`

```json
{"identifier": "01012345678", "password": "P@ssw0rd"}
```

`identifier` 는 휴대폰 번호 또는 이메일이다. 비밀번호 불일치와 존재하지 않는 계정을 **같은 응답**(`401 INVALID_CREDENTIALS`)으로 처리해 계정 존재 여부를 노출하지 않는다.

```json
{
  "userId": 1,
  "email": "test@example.com",
  "phone": "01000000000",
  "nickname": "테스터",
  "region": "서울특별시 강남구"
}
```

응답에 비밀번호는 담기지 않는다. 개발 시드 계정은 `01000000000` / `P@ssw0rd` 다.

> 토큰을 발급하지 않는다. 클라이언트는 `userId` 로 개발용 세션 토큰(`dev.<userId>.<timestamp>`)을 만들어 `Authorization: Bearer` 로 보내고, 서버는 `DevAuthenticationFilter` 가 그것을 읽는다. JWT 를 붙일 때 `AuthService` 가 토큰을 발급하도록 바꾸면 된다.

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
{
  "resultId": 12,
  "skinType": "건성피부",
  "keywords": ["푸석함", "건조함"],
  "description": "피부의 유분과 수분이 부족해 세안 후 당김이나 건조함을 쉽게 느낄 수 있어요."
}
```

`keywords` 와 `description` 은 판정된 피부타입별 고정 카피(`DiagnosisConstants.copyOf`)다. 결과 화면이 이 값을 그대로 렌더링한다.

판정 순서 (먼저 만족하는 조건에서 확정):

1. 민감 점수(2번+3번) ≥ 4 → `민감성`
2. 복합 점수(5번) ≥ 2 → `복합성`
3. \|건성 점수(4번+6번) − 지성 점수(1번+7번)\| ≤ 2 → `복합성`
4. 그 외 → 건성 점수가 크면 `건성`, 아니면 `지성`

저장 결과: `DIAGNOSIS_ANSWER` 에 7행, `DIAGNOSIS_RESULT` 에 1행(`result_summary` = 위 `description`). 응답의 `skinType` 은 판정값에 `"피부"` 를 붙인 표시용 문자열이고, DB `skin_type` 에는 접미사 없이 저장된다. AI 추천은 `result_summary` 를 진단 요약 입력으로 읽는다.

오류:

| status | code | 상황 |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `answers` 누락, 7개가 아님, 빈 문자열 포함, `userId` 누락 |
| 400 | `INVALID_DIAGNOSIS_ANSWERS` | 허용되지 않는 답변 문자열 (몇 번째 답변인지 메시지에 포함) |

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

| status | code | 상황 |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `skinmonName` 누락 또는 20자 초과, `userId`/`resultId` 누락 |
| 404 | `DIAGNOSIS_RESULT_NOT_FOUND` | `resultId` 에 해당하는 진단 결과 없음 |
| 500 | `SKINMON_APPEARANCE_NOT_FOUND` | 해당 피부타입/표정 외형이 `SKINMON_APPEARANCE` 에 없음(참조 데이터 준비 문제) |

`SKINMON.user_id` 가 유니크라서 한 사용자당 한 마리다. 이미 있으면 upsert 로 이름·진단 결과·외형을 갱신하고, 표정은 기존 값을 유지한다(재진단 시나리오).

### 스킨몽 조회

`GET /api/skinmon/{userId}` → `200 OK`

```json
{"skinmonId": 5, "skinmonName": "몽이", "skinType": "건성", "expressionType": "happy"}
```

스킨몽이 없으면 `404 SKINMON_NOT_FOUND`.

### 표정 변경

`PATCH /api/skinmon/{skinmonId}/expression` → `200 OK`

```json
{"expressionType": "sad"}
```

`happy` / `sad` 만 허용한다. 그 외 값은 `400 VALIDATION_ERROR`, 외형 행이 없으면 `400 INVALID_SKINMON_EXPRESSION`, 없는 스킨몽은 `404 SKINMON_NOT_FOUND`.

---

## 5. 홈 API

### 홈 화면 조회

`GET /api/home?userId=1&sido=서울특별시&gugun=강남구` → `200 OK`

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `userId` | int | O | 사용자 ID |
| `sido`, `gugun` | string | 조건부 | 지역명. 서버가 `kma-area-codes.csv` 로 지점코드를 찾는다. 클라이언트 권장 방식 |
| `areaNo` | string | 조건부 | 기상청 동네예보 지점코드를 직접 지정 |

`areaNo` 또는 `sido`+`gugun` 중 하나는 있어야 한다. 둘 다 없으면 `400 INVALID_REQUEST_PARAMETER`.

```json
{
  "uvIndex": 5.0,
  "dustIndex": null,
  "exposureRate": 12.6900,
  "maxUvToday": 7.0,
  "skinType": "건성",
  "expressionType": "happy",
  "hourlyForecast": [
    {"hourOffset": 0, "forecastAt": "2026-08-21T09:00:00", "value": 3.0},
    {"hourOffset": 3, "forecastAt": "2026-08-21T12:00:00", "value": 5.0},
    {"hourOffset": 6, "forecastAt": "2026-08-21T15:00:00", "value": 7.0}
  ]
}
```

`forecastAt` 은 각 구간의 절대 시각이다. 클라이언트는 이 값으로 그래프 x축 라벨을 만든다(`hourOffset` 만으로는 발표시각을 몰라 시각 라벨을 만들 수 없다).

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

| status | code | 상황 |
|---|---|---|
| 400 | `INVALID_REQUEST_PARAMETER` | `areaNo` 도 `sido`+`gugun` 도 없음 |
| 400 | `REGION_NOT_FOUND` | `kma-area-codes.csv` 에 없는 지역명 |
| 404 | `SKINMON_NOT_FOUND` | 해당 사용자의 스킨몽이 없음 |
| 502 | `ENVIRONMENT_UPSTREAM_ERROR` | 기상청 API 실패 |

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

## 8. 알림 API

모든 엔드포인트는 인증이 필요하다. JWT 인증 필터가 토큰의 사용자 ID 를 `Long` 타입 요청 속성 `authenticatedUserId` 로 넣어주는 것을 전제로 한다(1.1 참고: 현재 미구현).

오류는 1.2 의 공통 형식(`timestamp`, `status`, `code`, `message`, `path`)을 따른다.

### 알림 설정 조회

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

`notifications` 는 항상 `UV`, `DUST`, `ROUTINE` 을 이 순서로 포함한다. DB 행이 없는 타입은 `notificationId=null`, `enabled=false`, `times=[]`, 타임스탬프 null 로 내려간다.

### 예약 알림 생성

`POST /api/notifications` → `201 Created`

```json
{"type":"ROUTINE","enabled":true,"times":["08:00","21:00"]}
```

생성된 알림 객체를 반환한다. 같은 사용자/타입이 이미 있으면 `409 NOTIFICATION_ALREADY_EXISTS`.

### 예약 알림 교체

`PUT /api/notifications/{notificationId}` → `200 OK`

```json
{"enabled":false,"times":["09:00"]}
```

시간 목록 전체를 원자적으로 교체한다. `enabled=true` 면 최소 한 개의 시간이 필요하고, `enabled=false` 면 빈 목록을 허용한다.

| status | code | 상황 |
|---|---|---|
| 400 | `NOTIFICATION_TIME_REQUIRED` | `enabled=true` 인데 시간이 없음 |
| 400 | `DUPLICATE_NOTIFICATION_TIME` | 시간 중복 |
| 400 | `INVALID_NOTIFICATION_TIME` | `HH:mm` 형식 위반 |

### 예약 알림 삭제

`DELETE /api/notifications/{notificationId}` → `204 No Content`

없는 설정은 `404 NOTIFICATION_NOT_FOUND`, 다른 사용자의 설정 접근은 `403 NOTIFICATION_FORBIDDEN`.

### 자외선 위험 경보 on/off

`PUT /api/notifications/uv-risk-warning` → `200 OK`

```json
{"enabled":true}
```

### 알림 발송 지역

사용자가 명시적으로 선택한 **현재 알림 지역 하나**만 저장한다. 위치 이력을 남기거나 야외 활동을 추론하지 않는다.

- `GET /api/notifications/location` → `200 OK`, `{"sido":"서울특별시","gugun":"강남구"}`. 미설정이면 빈 본문.
- `PUT /api/notifications/location` → `200 OK`, 정규화된 지역을 반환. 요청 본문은 `{"sido":"서울특별시","gugun":"강남구"}`.

이 지역은 예약 UV, 예약 미세먼지, 자외선 위험 경보 발송에 공통으로 쓴다. ROUTINE 발송은 지역이 필요 없다.

### 디바이스 토큰

- `POST /api/notifications/devices` → `204 No Content`. 본문 `{"token":"ExpoPushToken[...]","platform":"ANDROID"}`
- `DELETE /api/notifications/devices` → `204 No Content`. 본문 `{"token":"ExpoPushToken[...]"}`

플랫폼은 `ANDROID`, `IOS`, `WEB`. 등록은 upsert 이며 이전에 비활성된 토큰을 다시 살린다. 해제는 soft disable 이다.

### 발송 정책

- 모든 스케줄은 `Asia/Seoul` 기준이다.
- UV: 설정한 시각마다 선택 지역의 자외선 지수와 등급을 보낸다.
- DUST: 설정한 시각마다 PM10, PM2.5 값과 등급을 보낸다.
- ROUTINE: 설정한 시각마다 루틴 리마인더를 보낸다.
- 자외선 위험 경보: 기상청 현재/근시점 예보를 확인해 자외선 지수가 6 이상(`높음`, `매우높음`, `위험`)일 때 발송한다. 사용자는 on/off 만 설정한다.
- 같은 예보에 대한 중복 발송을 막기 위해 `(user_id, forecast_at)` 발송 마커를 저장한다. 개인별 자외선 노출량이나 위치 이력은 저장하지 않는다.

Expo Push 발송에는 `EXPO_PUSH_ENABLED=true` 가 필요하다. EAS 에서 향상된 푸시 보안을 켰다면 `EXPO_ACCESS_TOKEN` 을 설정하고, 아니면 비워둬도 된다. 백엔드는 Expo Push Service 로 티켓을 보내고 최소 15분 후 receipt 를 확인한다. `DeviceNotRegistered` 토큰은 자동으로 비활성된다.

---

## 9. 루틴 · 케어메모 API

모든 루틴 엔드포인트는 인증된 `authenticatedUserId` 를 사용한다. 클라이언트는 사용자 ID 를 보내지 않는다.

### 날짜별 루틴 조회

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

완료 여부 조인, 개수, 정수로 반올림한 진행률은 백엔드가 계산한다. 아침/저녁 루틴이 없으면 `routineId` 는 `null`, 항목은 빈 배열로 내려간다. 잘못된 날짜는 `400 INVALID_DATE`.

### 루틴 생성

`POST /api/routines` → `201 Created`

```json
{"type":"MORNING"}
```

`MORNING`, `EVENING` 만 허용한다. 사용자/타입당 하나만 만들 수 있고 중복은 `409 ROUTINE_ALREADY_EXISTS`.

### 항목 관리

- `POST /api/routines/{routineId}/items` 본문 `{"name":"세안","detail":"미온수"}` → `201 Created`
- `PATCH /api/routine-items/{itemId}` 같은 필드 → `200 OK`
- `DELETE /api/routine-items/{itemId}` → `204 No Content` (soft delete)
- `PUT /api/routines/{routineId}/items/order` 본문 `{"itemIds":[103,101,102]}` → `200 OK`

순서 변경 요청은 활성 항목 전체를 정확히 한 번씩 포함해야 한다. 삭제된 항목은 조회에서 사라지지만 DB 에는 남는다.

### 날짜별 완료 처리

`PUT /api/routine-items/{itemId}/completion` → `200 OK`

```json
{"date":"2026-08-19","completed":true}
```

같은 엔드포인트로 `completed=false` 로 되돌린다. `(item_id, completion_date)` 가 유니크라서 행이 중복되지 않고 갱신된다.

### 케어메모

- 요청 날짜의 메모는 `GET /api/routines` 응답에 포함된다.
- `POST /api/care-memos` 본문 `{"date":"2026-08-19","content":"선크림 구매"}` → `201 Created`
- `PATCH /api/care-memos/{memoId}` 본문 `{"content":"립밤 구매"}` → `200 OK`
- `PUT /api/care-memos/{memoId}/completion` 본문 `{"completed":true}` → `200 OK`
- `DELETE /api/care-memos/{memoId}` → `204 No Content`

다른 사용자의 루틴 · 항목 · 완료 · 메모에 접근하면 `403`, 없는 리소스는 `404`.

---

## 10. AI 루틴 추천 API

구현은 완료됐으나 실제 OpenAI 연동 검증은 아직 하지 않은 상태다. 사용자는 인증된 `authenticatedUserId` 에서 얻고 클라이언트는 `userId` 를 보내지 않는다. 생성된 추천은 ID 가 없고 저장되지 않는다.

### 추천 생성

`POST /api/ai-routines/recommend` → `200 OK`

```json
{"latitude":37.5172,"longitude":127.0473}
```

좌표는 `EnvironmentQueryService` 호출에 필요한 요청 단위 입력이며 사용자 프로필 데이터가 아니다. 서비스는 인증 사용자의 최신 `DIAGNOSIS_RESULT` 를 읽고 자외선 · PM10 · PM2.5 와 RAG 컨텍스트를 합쳐 추천을 만든다. 결과는 저장하지 않는다.

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

`skinType` 은 `DIAGNOSIS_RESULT.skin_type`(`VARCHAR(20)`), `diagnosisResult` 는 nullable 인 `result_summary`(`VARCHAR(255)`) 값이다. 테이블에 완료 상태 컬럼이 없어서 최신 행은 `user_id` 기준 `ORDER BY diagnosed_at DESC, result_id DESC LIMIT 1` 로 고른다.

| status | code | 상황 |
|---|---|---|
| 400 | (검증 오류) | 좌표 누락 또는 범위 초과 |
| 404 | `DIAGNOSIS_RESULT_NOT_FOUND` | 진단 기록 없음 |

환경 조회가 실패하면 진단 결과와 RAG 만으로 생성하고 `environment.available=false` 로 응답한다. 값을 임의로 만들어 넣지 않는다.

응답 검증 규칙: 아침 1~5개, 저녁 1~5개, `reasons` 최대 3개. 이름은 1~20자, 상세는 1~30자, 각 섹션의 `order` 는 1부터 연속이어야 한다.

### 추천 조회

추천 조회 엔드포인트는 없다. 다시 생성하면 새로운 일회성 응답이 만들어진다.

### 화면에 표시된 추천을 내 루틴으로 저장

`POST /api/routines/from-ai` → `201 Created`

```json
{
  "morning": [{"order":1,"name":"약산성 클렌저","detail":"쌓인 노폐물 제거"}],
  "evening": [{"order":1,"name":"클렌징 오일","detail":"자외선 차단제 제거"}]
}
```

요청에는 화면에 표시되어 사용자가 저장하기로 선택한 항목만 담는다. 추천 ID, `reasons`, 진단/환경 데이터, 사용자 ID 는 포함하지 않는다. 두 배열이 동시에 비어 있을 수는 없다. 각 배열은 최대 5개, 이름은 필수 20자 이내, 상세는 필수 30자 이내, `order` 는 1부터 연속이어야 한다.

이 엔드포인트는 AI 추천을 저장하지도 않고 OpenAI 를 호출하지도 않는다. 한 트랜잭션 안에서 전달받은 항목을 일반 루틴 데이터로 변환한다. 인증 사용자의 `USER_ROUTINE` 을 재사용하거나 생성하고, `ROUTINE_ITEM` 에 이어붙이며, 같은 시간대·같은 요청 안에서 이름이 완전히 같은 항목은 건너뛴다. 기존 루틴 항목은 변경하지 않는다. 완료 행은 만들지 않으며 이후 체크는 기존 날짜별 완료 엔드포인트를 쓴다.

---

## 11. 알려진 통합 이슈

1. **인증 미구현.** `authenticatedUserId` 를 세팅하는 필터가 없어 루틴 · 케어메모 · 알림 · AI 추천(22개 엔드포인트)은 현재 호출되지 않는다.
2. **사용자 식별 방식 이원화.** 진단 · 스킨몽 · 홈 · 챗봇은 클라이언트가 `userId` 를 보낸다. 인증 도입 시 이 파라미터는 제거 대상이다.
3. ~~에러 시맨틱~~ **해결됨.** 진단 · 스킨몽 · 홈 · 챗봇의 검증 실패가 400/404 로 구분되고, 요청 본문은 Bean Validation 으로 먼저 걸러진다.
4. **홈 응답의 `dustIndex` 가 항상 null.** 7절의 미세먼지 API 와 연결되어 있지 않다.
5. **`POST /api/auth/login` 이 인증이 아니다.** 고정된 1번 계정을 그대로 반환하고 `password` 필드는 조회하지 않아 항상 null 이다. DTO 에서 이 필드를 제거해야 한다.
