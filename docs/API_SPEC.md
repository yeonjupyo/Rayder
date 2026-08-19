# Notification API

All endpoints require authentication. Until the authentication module is merged, the integration boundary is request attribute `authenticatedUserId`.

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
    }
  ],
  "uvExposureWarning": {
    "enabled": true,
    "createdAt": "2026-08-19T10:00:00",
    "updatedAt": "2026-08-19T10:00:00"
  }
}
```

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

The complete time list is replaced atomically. An empty list is valid. Duplicate times return `400 DUPLICATE_NOTIFICATION_TIME`; invalid `HH:mm` values return `400 INVALID_NOTIFICATION_TIME`.

## Delete scheduled setting

`DELETE /api/notifications/{notificationId}` → `204 No Content`

Missing settings return `404 NOTIFICATION_NOT_FOUND`; access by another user returns `403 NOTIFICATION_FORBIDDEN`.

## Update cumulative UV warning

`PUT /api/notifications/uv-exposure-warning` → `200 OK`

```json
{"enabled":true}
```

This preference has no time list.

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

# AI routine recommendation API (design; not implemented)

All endpoints derive the user from authenticated `authenticatedUserId` (JWT integration boundary). Clients never send `userId`. Generated recommendations have no ID and are not persisted.

## Generate recommendation

Proposed endpoint: `POST /api/ai-routines/recommend` -> `200 OK`

```json
{"latitude":37.5172,"longitude":127.0473}
```

Coordinates are required request-scoped inputs for `EnvironmentQueryService`, not user-profile data. The service reads the authenticated user's latest `DIAGNOSIS_RESULT`, then combines it with UV, PM10, PM2.5, and later RAG context. It does not persist its output.

```json
{
  "statusSummary": {
    "skinType": "DRY",
    "diagnosisResult": "...",
    "environmentAvailable": true,
    "environment": {
      "region": "서울특별시 강남구",
      "uv": {"value":7.0,"level":"높음","observedAt":"2026-08-19T15:00:00"},
      "pm10": {"value":18.0,"level":"좋음","observedAt":"2026-08-19T15:00:00"},
      "pm25": {"value":9.0,"level":"좋음","observedAt":"2026-08-19T15:00:00"}
    }
  },
  "reasons": ["수분 보충과 장벽 관리가 필요해요."],
  "morning": [{"order":1,"name":"약산성 클렌저","detail":"쌓인 노폐물 제거"}],
  "evening": [{"order":1,"name":"클렌징 오일","detail":"자외선 차단제 제거"}]
}
```

`skinType` is the `VARCHAR(20)` value from `DIAGNOSIS_RESULT.skin_type`; `diagnosisResult` is the nullable `VARCHAR(255)` value from `result_summary`. Since the table has no completion-status column, the latest row is selected by `user_id` with `ORDER BY diagnosed_at DESC, result_id DESC LIMIT 1`. Invalid/missing coordinates return `400`; no diagnosis returns `404 DIAGNOSIS_RESULT_NOT_FOUND`. If environment lookup fails, generation falls back to diagnosis plus RAG: `environmentAvailable` is false, `environment` is null, and the model must not invent environmental values.

Response validation requires at most 5 morning items, at most 5 evening items, and at most 3 reasons. Either routine array may be empty, although generation instructs the model to produce at least one item in both when possible. Names are 1–30 characters, details at most 100 characters, and each section's order is consecutive from 1.

## Read recommendation

There is no recommendation read endpoint. Retrying generation creates a new transient response.

## Save displayed recommendation as user routine

Proposed endpoint: `POST /api/routines/from-ai`. This is a transactional batch endpoint distinct from the existing single routine/item APIs.

```json
{
  "morning": [{"order":1,"name":"약산성 클렌저","detail":"쌓인 노폐물 제거"}],
  "evening": [{"order":1,"name":"클렌징 오일","detail":"자외선 차단제 제거"}]
}
```

The request contains only the recommendation items currently shown by the frontend; it contains no recommendation ID, `statusSummary`, reasons, diagnosis/environment data, or user ID. It is validated with the same item count, length, and consecutive-order rules as the response. Both arrays may not be empty simultaneously.

Saving does not call OpenAI. After authentication and ownership checks, the backend reuses or creates each time's `USER_ROUTINE`, skips any item whose exact `name` already exists actively in that same time (also deduplicating exact names within the request), and appends the remainder after existing items in one transaction. Completion state is not saved; date-specific completion begins independently through the existing completion API. The response should return the resulting morning/evening routine groups.
