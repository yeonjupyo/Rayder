# Backend implementation status

Audit date: 2026-08-20. This report is based on the current controllers, services, DTOs, MyBatis SQL, migration SQL, tests, build output, and packaged Boot JAR. Existing design documents were treated as secondary evidence.

## Executive summary

- The project contains 26 HTTP endpoints: 5 environment, 5 notification, 8 routine, 4 care-memo, 1 AI recommendation, and 3 example endpoints.
- Environment, notification, routine, AI/RAG, OpenAI client, and AI-to-routine conversion code exist.
- The frontend cannot yet call the 18 user-specific endpoints end-to-end because this repository contains no JWT/security component that creates the required `authenticatedUserId` request attribute.
- `gradlew.bat clean build` succeeded. 28 tests passed and 3 conditional live tests were skipped (2 MariaDB, 1 OpenAI).
- KMA/AirKorea/Kakao, live MariaDB, and live OpenAI success were not exercised in this audit. They are not marked complete.
- Migration files V3/V4 are alterations of pre-existing tables. This repository does not contain the baseline DDL for `USER`, `DIAGNOSIS_RESULT`, `NOTIFICATION_SETTING`, `USER_ROUTINE`, `ROUTINE_ITEM`, or `CARE_MEMO`, and Flyway/Liquibase is not configured. Schema application is therefore an external/manual prerequisite.

Status meanings: `COMPLETE` requires code, schema, and relevant executed tests; `PARTIAL` has implementation but lacks live/external verification; `BLOCKED` cannot be called through its intended contract because another component or configuration is absent.

## Runtime structure

`Controller -> Service -> MyBatis Mapper -> MariaDB` is used for notifications and routines. Environment requests use `EnvironmentQueryService -> Kakao/KMA/AirKorea clients`. AI recommendation uses latest diagnosis + environment lookup/fallback + PDF vector retrieval + OpenAI Responses API. Generated recommendations and retrieved chunks are not persisted; only `POST /api/routines/from-ai` writes selected items.

## Endpoint inventory

All DTO field names shown below are JSON names. `auth` means a request attribute named `authenticatedUserId`; there is no implemented bearer-token parser in this repository.

| # | Function | Method and URL | Auth | Input | Response / success | Main service and tables | Main errors | Status |
|---:|---|---|---|---|---|---|---|---|
| 1 | UV by region | `GET /api/environment/uv` | No | query `sido`, `gugun` | `EnvironmentInfo`, 200 | `EnvironmentQueryService`; no DB | `INVALID_ENVIRONMENT_REQUEST`, `REGION_NOT_FOUND`, `ENVIRONMENT_UPSTREAM_ERROR` | PARTIAL |
| 2 | PM10/PM2.5 by region | `GET /api/environment/dust` | No | query `sido`, `gugun` | `EnvironmentInfo[]`, 200 | same; no DB | same | PARTIAL |
| 3 | UV by coordinates | `GET /api/environment/uv/by-location` | No | query `lat` double, `lon` double | `EnvironmentInfo`, 200 | same; no DB | same | PARTIAL |
| 4 | PM10/PM2.5 by coordinates | `GET /api/environment/dust/by-location` | No | query `lat`, `lon` | `EnvironmentInfo[]`, 200 | same; no DB | same | PARTIAL |
| 5 | coordinates to region | `GET /api/location` | No | query `lat`, `lon` | `GeoRegion`, 200 | same; no DB | same | PARTIAL |
| 6 | list notification settings | `GET /api/notifications` | Required | none | `NotificationListResponse`, 200 | `NotificationService`; `USER`, `NOTIFICATION_SETTING`, `NOTIFICATION_TIME`, `NOTIFICATION_WARNING_SETTING` | `USER_NOT_FOUND` | BLOCKED |
| 7 | create scheduled setting | `POST /api/notifications` | Required | body `NotificationSettingRequest` | `NotificationSettingResponse`, 201 | same | `NOTIFICATION_ALREADY_EXISTS`, time/validation errors | BLOCKED |
| 8 | replace enabled/times | `PUT /api/notifications/{notificationId}` | Required | path ID; body `NotificationUpdateRequest` | `NotificationSettingResponse`, 200 | same | not found/forbidden/time errors | BLOCKED |
| 9 | delete scheduled setting | `DELETE /api/notifications/{notificationId}` | Required | path ID | empty, 204 | same | `NOTIFICATION_NOT_FOUND`, `NOTIFICATION_FORBIDDEN` | BLOCKED |
| 10 | set UV exposure warning | `PUT /api/notifications/uv-exposure-warning` | Required | body `WarningSettingRequest` | `WarningSettingResponse`, 200 | same | `USER_NOT_FOUND`, `WARNING_SETTING_SAVE_FAILED` | BLOCKED |
| 11 | daily routine view | `GET /api/routines` | Required | query `date=yyyy-MM-dd` | `MyRoutineResponse`, 200 | `RoutineService`; routine, item, completion, memo tables | `INVALID_DATE`, `USER_NOT_FOUND` | BLOCKED |
| 12 | create morning/evening routine | `POST /api/routines` | Required | body `RoutineCreateRequest` | `RoutineGroupResponse`, 201 | same | `ROUTINE_ALREADY_EXISTS`, validation | BLOCKED |
| 13 | save selected AI items | `POST /api/routines/from-ai` | Required | body `AiRoutineSaveRequest` | `AiRoutineSaveResponse`, 201 | same; `USER_ROUTINE`, `ROUTINE_ITEM` | `INVALID_AI_ROUTINE`, validation | BLOCKED |
| 14 | add routine item | `POST /api/routines/{routineId}/items` | Required | path ID; body `RoutineItemCreateRequest` | `RoutineItemResponse`, 201 | same | routine not found/forbidden, validation | BLOCKED |
| 15 | edit routine item | `PATCH /api/routine-items/{itemId}` | Required | path ID; body `RoutineItemUpdateRequest` | `RoutineItemResponse`, 200 | same | item not found/forbidden, validation | BLOCKED |
| 16 | soft-delete routine item | `DELETE /api/routine-items/{itemId}` | Required | path ID | empty, 204 | same | item not found/forbidden | BLOCKED |
| 17 | replace item order | `PUT /api/routines/{routineId}/items/order` | Required | path ID; body `RoutineOrderRequest` | `RoutineItemResponse[]`, 200 | same | `INVALID_ROUTINE_ORDER`, ownership errors | BLOCKED |
| 18 | set daily item completion | `PUT /api/routine-items/{itemId}/completion` | Required | path ID; body `RoutineCompletionRequest` | `RoutineItemResponse`, 200 | same; `ROUTINE_ITEM_COMPLETION` | item not found/forbidden, validation | BLOCKED |
| 19 | create care memo | `POST /api/care-memos` | Required | body `CareMemoCreateRequest` | `CareMemoResponse`, 201 | `RoutineService`; `USER`, `CARE_MEMO` | user not found, validation | BLOCKED |
| 20 | edit care memo | `PATCH /api/care-memos/{memoId}` | Required | path ID; body `CareMemoUpdateRequest` | `CareMemoResponse`, 200 | same | memo not found/forbidden, validation | BLOCKED |
| 21 | set memo completion | `PUT /api/care-memos/{memoId}/completion` | Required | path ID; body `CareMemoCompletionRequest` | `CareMemoResponse`, 200 | same | memo not found/forbidden, validation | BLOCKED |
| 22 | delete care memo | `DELETE /api/care-memos/{memoId}` | Required | path ID | empty, 204 | same | memo not found/forbidden | BLOCKED |
| 23 | generate AI recommendation | `POST /api/ai-routines/recommend` | Required | body `AiRoutineRecommendationRequest` | `AiRoutineRecommendationResponse`, 200 | AI service; reads `DIAGNOSIS_RESULT`; external APIs | diagnosis/RAG/OpenAI errors | BLOCKED |
| 24 | list examples | `GET /api/examples` | No | none | `Example[]`, 200 | `ExampleService`; `example` | generic errors | PARTIAL |
| 25 | get example | `GET /api/examples/{id}` | No | path ID | `Example`, 200 | same | generic errors | PARTIAL |
| 26 | create example | `POST /api/examples` | No | query `name` | `Example`, 200 | same | parameter/generic errors | PARTIAL |

`PARTIAL` on environment means implementation and mock/unit coverage exist but live providers were not called. `BLOCKED` on authenticated endpoints describes frontend readiness, not absence of controller/service code. Example endpoints are scaffold/demo code and have no dedicated tests.

## Feature audit

### Authentication and diagnosis

- No Spring Security dependency, JWT filter, login controller, or code that sets `authenticatedUserId` exists. The attribute contract is consistent across notification, routine, memo, and AI controllers, but integration is pending.
- The AI mapper reads `DIAGNOSIS_RESULT(result_id, user_id, skin_type, result_summary, diagnosed_at)` and aliases `result_summary AS diagnosis_result`.
- Latest selection is correctly `ORDER BY diagnosed_at DESC, result_id DESC LIMIT 1`. No diagnosis write API exists in this repository.

### Environment

- Region CSV loading, region lookup, Kakao coordinate conversion, KMA UV, AirKorea PM10 and PM2.5, timeouts, provider-response checks, and domain error translation exist.
- Coordinate range validation is implemented inside Kakao and in the AI request DTO. Direct environment endpoints use primitive query parameters and rely on the Kakao client for finite/range validation on location flows.
- `EnvironmentQueryService` is reused by AI. AI catches any environment runtime failure and continues with `available=false`; direct environment endpoints return errors instead.
- No retry is implemented for KMA/AirKorea/Kakao. No live-provider test ran during this audit.

### Notifications

- List, create, full replacement of enabled/times, delete, and independent cumulative-UV warning preference exist.
- `HH:mm` strict parsing, duplicate detection, ownership checks, user existence checks, and transaction boundaries exist.
- There are no separate add-time/delete-time endpoints; `PUT` atomically replaces the complete list. Actual push/scheduling delivery is not implemented.

### Routines and memos

- Permanent morning/evening groups, item CRUD, soft deletion, exact-set reorder validation, date-specific completion upsert, daily progress, and ownership checks exist.
- Care memos are returned inside the daily routine view and support create/edit/completion/delete.
- AI-selected items can be appended transactionally. Existing exact-name duplicates are skipped; AI recommendations themselves remain transient.

### RAG and OpenAI

The complete code path exists:

`classpath PDF -> PDFBox extraction -> whitespace normalization -> overlapping chunks -> lazy document embeddings -> in-memory vector cache -> query embedding -> cosine threshold/rank -> top K context -> Responses API structured output -> application validation`.

- PDF load and text extraction occur in the retriever constructor; a missing, unreadable, or empty PDF prevents application context creation with `IllegalStateException`.
- Document embeddings are created lazily on the first recommendation and cached only in memory. Every process restart causes PDF parsing during startup and document embedding calls on first retrieval.
- Embedding failures become OpenAI/business errors. Empty retrieval becomes `RAG_CONTEXT_NOT_FOUND`. Dimension mismatch becomes `RAG_RETRIEVAL_ERROR`.
- Embedding model is `text-embedding-3-small`; response model is `gpt-4o-mini`; response requests set `store=false` and strict JSON Schema.
- Connect/read timeouts are 3s/30s. At most 2 immediate attempts are made for 429, 5xx, and transport failures; there is no backoff.
- The live OpenAI test exists but was skipped because `OPENAI_API_KEY` was not present in the process environment.

## Database mapping status

| Table | Use | Evidence / caveat |
|---|---|---|
| `USER` | existence and owner FK target | Referenced by SQL; baseline DDL absent here |
| `DIAGNOSIS_RESULT` | latest diagnosis read | Mapper and documented live shape; baseline DDL absent here |
| `NOTIFICATION_SETTING` | one setting per user/type | V3 alters pre-existing table; unique/check/FK added |
| `NOTIFICATION_TIME` | distinct setting times | V3 creates table; unique and cascading FK |
| `NOTIFICATION_WARNING_SETTING` | independent warning flag | V3 creates table; user PK/FK cascade |
| `USER_ROUTINE` | one group per user/time type | V4 alters pre-existing table; unique/check/FK |
| `ROUTINE_ITEM` | ordered, soft-deleted items | V4 alters pre-existing table; baseline constraints absent here |
| `ROUTINE_ITEM_COMPLETION` | per-date completion | V4 creates table; unique `(item_id, completion_date)`; FK has no cascade |
| `CARE_MEMO` | per-date memo/check | V4 alters pre-existing table; user/date index and cascade FK |
| `request` | asynchronous failed-request logs | V2 creates table |
| `example` | scaffold endpoint | V1 creates table |

Mapper names and V3/V4 columns agree. However, there is no migration runner dependency and V3/V4 assume legacy tables/columns already exist. The live database was not queried because `DB_TEST_ENABLED` and DB environment variables were absent from the process. Therefore repository SQL compatibility is verified, but deployed constraints are not re-certified here.

## Configuration

| Variable | Purpose | Required | Local execution |
|---|---|---|---|
| `DATA_GO_KR_SERVICE_KEY` | KMA and AirKorea | Required for environment calls and environment-enabled AI | Required for those calls |
| `KAKAO_REST_API_KEY` | coordinate to region | Required for location endpoints and AI environment lookup | Required for those calls |
| `OPENAI_API_KEY` | embeddings and Responses API | Required only when recommendation/RAG is invoked | Required for AI/live test |
| `DB_URL` | production JDBC URL | Required with `prod` profile | Not used by test profile |
| `DB_USERNAME`, `DB_PASSWORD` | production DB credentials | Required with `prod` profile | DB tests require them |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | conditional DB-test JDBC components | Required only for DB integration tests | With `DB_TEST_ENABLED=true` |
| `DB_TEST_ENABLED` | enables destructive transactional DB tests | Optional; must equal `true` | Test only |

No JWT-related variable is referenced in this repository. The local `.env` contains names for DB/provider/OpenAI settings, but application code does not automatically load `.env`; variables must be exported by the launcher/IDE. No secret values were inspected or recorded.

## Test and build evidence

`gradlew.bat clean build --console=plain`: **BUILD SUCCESSFUL** on 2026-08-20.

- Passed: application context 1; AI mock/service 5; environment unit 6; notification unit 8; routine unit 8. Total passed: 28.
- Skipped: live OpenAI 1 (`OPENAI_API_KEY` absent); notification MariaDB 1 and routine MariaDB 1 (`DB_TEST_ENABLED` absent). Total skipped: 3.
- No controller/API E2E tests, JWT tests, or live KMA/AirKorea/Kakao tests exist.
- The built JAR contains `rag/Rayder_RAG.pdf`, `kma-area-codes.csv`, and all mapper XML files.

## Code quality and cleanup candidates

- `uv dust api_2/` duplicates an older environment implementation under a different package and is not part of the Gradle source set. It is a deletion/archive candidate after ownership confirmation.
- `example` controller/service/mapper/schema are scaffold artifacts unless intentionally retained.
- `AiRoutineLiveIntegrationTest` validates live OpenAI/RAG generation but replaces diagnosis/environment collaborators, so it is not a full HTTP/JWT/DB/provider E2E test.
- Request logging redacts headers through `JsonLogSupport`; no literal provider/OpenAI secrets were found in main configuration. Test-only passwords are fixed fixtures.
- No TODO/FIXME was found in backend production Java. Existing modified/untracked files were preserved during this audit.

## Document mismatches

### [DOCUMENT MISMATCH] API_SPEC.md

- Document: called the AI recommendation API “design; not implemented”.
- Actual code: controller, orchestration, diagnosis mapper, RAG, embeddings, Responses API, validation, tests, and save conversion exist.
- Difference: implementation status was stale.
- Action: heading corrected; live verification remains partial.

### [DOCUMENT MISMATCH] AI_RAG_DESIGN.md

- Document: one example used a nested `statusSummary` and detailed environment object.
- Actual code: top-level `skinType`, `diagnosisResult`, and `environment {available, uvLevel, dustLevel}`.
- Difference: obsolete response example.
- Action: the frontend guide and this audit define the actual DTO; the design document should be treated as rationale where it conflicts.

### [DOCUMENT MISMATCH] expected document set

- `PROJECT_OVERVIEW.md` and `DEVELOPMENT_GUIDE.md` named in the request are absent.
- V3/V4 migration files are not an executable full schema history despite documentation wording that can imply a self-contained database setup.

