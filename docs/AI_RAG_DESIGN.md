# AI/RAG routine recommendation design

## Scope and decision status

This document fixes the API and persistence boundary before OpenAI or RAG implementation. **Confirmed** items are supported by the current repository, frontend, or database mapping. **Proposed** items are the contract for later implementation. **Open** items must not be implemented until the missing product/schema decision is supplied.

No `AI_RECOMMENDATION`, `AI_RECOMMENDATION_ITEM`, or recommendation-history table is introduced. A generated recommendation is transient until the user explicitly saves the payload as a user routine.

## Recommendation input

### User identity — confirmed

The authenticated `userId` comes from JWT authentication. As with the existing routine controllers, the application boundary is currently the `authenticatedUserId` request attribute until the JWT module is merged. Neither request body nor query parameters accept `userId`.

`userId` finds the user's diagnosis and authorizes a later routine save. Location is a separate input and is not inferred from, or permanently attached to, the user.

### Latest diagnosis — confirmed

Read only the latest diagnosis row for the authenticated user from `DIAGNOSIS_RESULT`, without deleting or overwriting older rows. Recommendation input is limited to the existing skin-type value and diagnosis-result value; do not invent skin-condition/keyword fields or add columns.

The live MariaDB schema contains no completion-status column. Each row is therefore treated as a completed diagnosis. Its actual columns are `result_id INT` (PK, auto increment), `user_id INT` (FK to `USER.user_id`), `skin_type VARCHAR(20) NOT NULL`, `result_summary VARCHAR(255) NULL`, and `diagnosed_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP`.

Select the latest row for the authenticated user with `ORDER BY diagnosed_at DESC, result_id DESC LIMIT 1`. MariaDB places nulls after non-null timestamps for descending order; if legacy rows have equal or null `diagnosed_at`, descending `result_id` supplies the deterministic tie-breaker. No status predicate or speculative column is added.

```sql
SELECT result_id, user_id, skin_type, result_summary, diagnosed_at
FROM DIAGNOSIS_RESULT
WHERE user_id = #{userId}
ORDER BY diagnosed_at DESC, result_id DESC
LIMIT 1;
```

If no completed diagnosis exists, the recommendation endpoint should return a domain `404` (proposed code: `DIAGNOSIS_RESULT_NOT_FOUND`) rather than generate an unpersonalized routine.

### Environment — confirmed

AI orchestration reuses `EnvironmentQueryService`; it does not call KMA, AirKorea, or Kakao clients directly. `EnvironmentInfo` is:

| Field | Java type | Meaning |
|---|---|---|
| `type` | `EnvironmentInfo.Type` | `UV`, `DUST_PM10`, or `DUST_PM25` |
| `value` | `Double` | Observed/index value |
| `level` | `String` | Provider-normalized qualitative level |
| `region` | `String` | Resolved region label |
| `observedAt` | `LocalDateTime` | Observation/base time |

Use one UV value from `getUvByLocation(lat, lon)` and the PM10/PM2.5 values returned by `getDustByLocation(lat, lon)`. The service also supports `sido/gugun`, but coordinates are the proposed AI request contract because the backend already owns Kakao region resolution and the frontend need not duplicate it.

The frontend sends `latitude` and `longitude` for each recommendation request. They are request-scoped environment lookup inputs and are not persisted in the user database. Validate required fields and coordinate ranges at the API boundary.

## Structured recommendation result — confirmed contract

The frontend dummy model and Figma-linked screen establish skin type and a diagnosis/condition summary, an environment status summary, morning/evening tabs, ordered `name`/`detail` rows, and recommendation reasons displayed as one list below the routine card.

Reasons belong to the whole recommendation, not individual items. The response contains `statusSummary`, `morning`, `evening`, and `reasons`. Morning and evening are both generated and should each contain at least one item when possible, but either array may be empty. Each array has at most 5 items; `reasons` has at most 3. Item `name` is 1–30 characters, `detail` is at most 100 characters, and `order` starts at 1 and is consecutive within each time section.

```json
{
  "statusSummary": {
    "skinType": "DRY",
    "diagnosisResult": "...existing diagnosis result value...",
    "environmentAvailable": true,
    "environment": {
      "region": "서울특별시 강남구",
      "uv": {"value": 7.0, "level": "높음", "observedAt": "2026-08-19T15:00:00"},
      "pm10": {"value": 18.0, "level": "좋음", "observedAt": "2026-08-19T15:00:00"},
      "pm25": {"value": 9.0, "level": "좋음", "observedAt": "2026-08-19T15:00:00"}
    }
  },
  "reasons": ["수분 보충과 장벽 관리가 필요해요."],
  "morning": [{"order": 1, "name": "약산성 클렌저", "detail": "쌓인 노폐물 제거"}],
  "evening": [{"order": 1, "name": "클렌징 오일", "detail": "자외선 차단제 제거"}]
}
```

`skinType` maps from `skin_type` as a string of at most 20 characters, and `diagnosisResult` maps from nullable `result_summary` as a string of at most 255 characters. The response must preserve existing values rather than introduce a speculative enum or column.

## Transient recommendation and save flow — confirmed

```text
JWT userId -> latest DIAGNOSIS_RESULT --------------+
latitude/longitude -> EnvironmentQueryService -----+-> prompt input
RAG query -> retrieved context --------------------+-> OpenAI structured output
                                                    -> response (not persisted)
                                                    -> frontend review
frontend sends displayed morning/evening items -----> USER_ROUTINE / ROUTINE_ITEM
```

There is no recommendation ID and no recommendation read endpoint. The save request contains the displayed morning/evening items. On save they become ordinary routine items and may be added, edited, soft-deleted, reordered, and checked per date. Later edits neither update nor refer back to the transient recommendation. `is_ai_recommended` may be true as existing provenance metadata; it is not a history relationship.

An AI save needs a dedicated transactional batch operation (proposed `POST /api/routines/from-ai`) rather than many calls to the current single-item endpoint. It creates missing morning/evening `USER_ROUTINE` rows and inserts validated items into `ROUTINE_ITEM` with submitted order.

The save policy is **append**. Reuse an existing `USER_ROUTINE` for the same time or create it when absent, then append new active items after the current maximum order. If an active item in that same time section has exactly the same `name`, skip it. Name comparison is exact; do not apply fuzzy, case-insensitive, or `detail` comparison. Completion state is not copied from the recommendation and begins independently for each date through the existing completion table.

The frontend payload is untrusted input. Saving never calls OpenAI again, but the backend must validate DTO shape, lengths, counts, and consecutive order; authenticate the user and verify routine ownership; remove same-time exact-name duplicates (including duplicates inside the request); and persist the remaining items atomically. Only `morning` and `evening` items are saved. `statusSummary`, `reasons`, diagnosis, and environment data are not accepted or persisted by the save endpoint.

## Environment failure fallback — confirmed

An environment provider failure does not prevent recommendation generation. Continue with the latest diagnosis and RAG knowledge, set `statusSummary.environmentAvailable` to false and `environment` to null, and include this prompt rule:

> 환경정보를 조회할 수 없는 상태입니다. 피부진단 결과와 검색된 피부 관리 지식만을 기반으로 추천하세요.

The model must not invent UV, PM10, PM2.5, their levels, region, or observation time. This fallback applies to environment lookup/provider failure; a missing diagnosis still returns `DIAGNOSIS_RESULT_NOT_FOUND`.

## RAG connection point — proposed, not implemented

Build the search query after diagnosis and environment retrieval from the confirmed diagnosis values plus UV, PM10, and PM2.5 values/levels. Add retrieved evidence to the OpenAI prompt alongside those inputs, before structured response generation. Vector storage, embeddings, retrieval implementation, and OpenAI calls remain out of scope.
