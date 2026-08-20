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

This preference has no time list. Users configure only `enabled`; the dangerous UV threshold is system policy and is not stored per user.

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
