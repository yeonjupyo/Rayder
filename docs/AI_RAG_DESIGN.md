# AI/RAG routine recommendation design

## Scope and decision status

This document describes the implemented first version of the OpenAI/RAG recommendation flow. Generated recommendations and retrieved evidence remain transient.

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

Reasons belong to the whole recommendation, not individual items. The response contains `skinType`, `diagnosisResult`, `environment`, `morning`, `evening`, and `reasons`. Morning and evening each contain 1–5 items; `reasons` has at most 3. Item `name` is 1–20 characters, `detail` is 1–30 characters, and `order` starts at 1 and is consecutive within each time section.

```json
{
  "skinType": "DRY",
  "diagnosisResult": "...existing diagnosis result value...",
  "environment": {
    "available": true,
    "uvLevel": "높음",
    "dustLevel": "좋음 / 좋음"
  },
  "reasons": ["수분 보충과 장벽 관리가 필요해요."],
  "morning": [{"order": 1, "name": "약산성 클렌저", "detail": "쌓인 노폐물 제거"}],
  "evening": [{"order": 1, "name": "클렌징 오일", "detail": "자외선 차단제 제거"}]
}
```

`skinType` maps from `skin_type` as a string of at most 20 characters, and `diagnosisResult` maps from nullable `result_summary` as a string of at most 255 characters. The response must preserve existing values rather than introduce a speculative enum or column.

## Transient recommendation and explicit conversion flow

```text
JWT userId -> latest DIAGNOSIS_RESULT --------------+
latitude/longitude -> EnvironmentQueryService -----+-> prompt input
RAG query -> retrieved context --------------------+-> OpenAI structured output
                                                    -> response (not persisted)
                                                    -> frontend review
user selects checklist save -----------------------> USER_ROUTINE / ROUTINE_ITEM
```

There is no recommendation ID and no recommendation read endpoint. The response, diagnosis input, environment input, and retrieved evidence are never persisted by the AI feature.

Generation never writes to `ROUTINE_ITEM`. Only after the user explicitly selects “체크리스트 저장하기” does `POST /api/routines/from-ai` convert the submitted morning/evening items into ordinary user routine data. The operation reuses or creates the authenticated user's `USER_ROUTINE` for each time type and appends items to the existing `ROUTINE_ITEM` structure in one transaction.

The submitted payload is untrusted and is validated independently of the earlier response. Each section has at most five items, order is consecutive from 1, names are at most 20 characters, details are at most 30 characters, and both sections cannot be empty. Active items with an exactly equal name in the same time section are skipped; exact duplicates inside the request are also stored once. Existing routine items are never replaced. `is_ai_recommended=true` records provenance only and does not create a recommendation-history relationship.

Conversion does not create completion rows. Saved entries subsequently use the existing date-specific completion flow exactly like manually entered general routine items.

## Environment failure fallback — confirmed

An environment provider failure does not prevent recommendation generation. Continue with the latest diagnosis and RAG knowledge, return `environment.available=false`, and include this prompt rule:

> 환경정보를 조회할 수 없는 상태입니다. 피부진단 결과와 검색된 피부 관리 지식만을 기반으로 추천하세요.

The model must not invent UV, PM10, PM2.5, their levels, region, or observation time. This fallback applies to environment lookup/provider failure; a missing diagnosis still returns `DIAGNOSIS_RESULT_NOT_FOUND`.

## RAG and OpenAI implementation

- Knowledge source: project-supplied `src/main/resources/rag/Rayder_RAG.pdf`.
- Extraction/chunking: Apache PDFBox, whitespace normalization, 900 characters with 150-character overlap.
- Embedding: OpenAI `text-embedding-3-small`; document vectors initialize lazily and remain cached in memory.
- Vector store/retrieval: in-process cosine search, similarity threshold 0.15, top 4. This avoids introducing another database or service for the single-document MVP.
- Generation: OpenAI Responses API with `gpt-4o-mini`, `store=false`, and strict JSON Schema Structured Outputs.
- Resilience: 3-second connect timeout, 30-second read timeout, and at most two attempts for 429, 5xx, and transport/timeout failures.

The retrieval query combines diagnosis values with available UV, PM10, and PM2.5 values/levels. Retrieved chunks are inserted into the separated `[RAG CONTEXT]` prompt section. Empty retrieval returns `RAG_CONTEXT_NOT_FOUND`; the parsed model output is validated again before it is returned.
