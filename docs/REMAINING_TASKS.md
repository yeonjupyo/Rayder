# Remaining backend and integration tasks

Priorities reflect the current code audit. “Owner” describes the responsible area, not an assigned person.

## P0 — required for the app to work end-to-end

| Task | Why | Current blocker | Owner | Prerequisite / done condition |
|---|---|---|---|---|
| Merge JWT/authentication integration | 18 product endpoints require `authenticatedUserId` | No security/JWT code creates the request attribute | Auth/backend | Valid bearer token sets trusted numeric user ID; missing/invalid tokens return stable 401/403; API E2E test passes |
| Confirm/apply full MariaDB schema | Services depend on legacy tables before V3/V4 | Baseline DDL and migration runner are absent; DB tests skipped | DB/backend | Versioned full schema or verified deployment procedure; V3/V4 applied; constraints queried from MariaDB |
| Run notification/routine DB integration tests | Mapper/schema compatibility is otherwise unproven in this environment | `DB_TEST_ENABLED` and process DB variables absent | Backend/DB | Both conditional test classes execute (not skip) and pass on an isolated DB |
| Integrate frontend API client | Product screens still use dummy/in-memory data | Auth contract and reusable API client missing | Frontend/auth | Base URL, bearer interceptor, error mapping, and screen calls use documented endpoints |
| Provide real diagnosis flow/data | AI returns 404 without a diagnosis row | Diagnosis API is owned elsewhere and absent here | Diagnosis/backend | Authenticated user can complete diagnosis and latest row matches mapper contract |
| Verify live AI recommendation | AI screen depends on embeddings and Responses API | Live test skipped; key and network not present | AI/backend/ops | Real embedding + retrieval + Responses call passes with controlled account/cost and logs no secret |

## P1 — required to claim core features complete

| Task | Why | Current blocker | Owner | Prerequisite / done condition |
|---|---|---|---|---|
| Live-test Kakao, KMA UV, AirKorea PM10/PM2.5 | Only mock/validation coverage exists | Provider credentials/network and stable fixtures required | Environment/backend | Region and coordinate paths pass; provider failure codes/fallback verified |
| Add HTTP/controller E2E tests | Service tests do not prove serialization, status, auth, or error bodies | JWT module not integrated | Backend/QA | Happy/error cases cover all product controllers with real request boundary |
| Validate production `.env` loading strategy | Spring does not automatically load repository `.env` | Launcher/IDE/deployment ownership unclear | Ops/backend | Documented secret injection for local and prod; startup failure is clear for required provider vars |
| Verify live schema constraints/indexes | Repository migrations assume prior shapes | Live metadata not queried in this audit | DB/backend | PK/FK/unique/index/nullability/on-delete report matches mapper assumptions |
| RAG quality and failure tests | Code path exists, but retrieval relevance and PDF failure cases lack direct tests | Live embedding cost and quality dataset | AI/QA | Golden queries, threshold/top-K review, empty/corrupt PDF and embedding-failure tests pass |
| Decide fate of example endpoints | Demo API expands public surface and schema | Product ownership not stated | Backend | Remove in a separate approved change or explicitly retain/document |
| Implement notification delivery | Current feature stores settings only | Scheduler/push provider/device-token design absent | Notification/backend | Due notifications are delivered with retry/observability and consent handling |

## P2 — follow-up improvements

| Task | Why | Current blocker | Owner | Prerequisite / done condition |
|---|---|---|---|---|
| Persist/version embedding cache | Restart causes PDF parse and first-request document re-embedding | No cache/vector-store design | AI/ops | Cache keyed by PDF hash + model; safe invalidation and cold-start metrics |
| Add OpenAI retry backoff/jitter | Current two attempts are immediate | Retry policy not designed | AI/backend | Bounded exponential backoff, request observability, and rate-limit tests |
| Add RAG metadata/citations | Current chunks are plain strings | PDF page/source metadata discarded | AI/backend | Retrieved context records source/page and can be audited |
| Add composite latest-diagnosis index if justified | Query orders by user/time/result | Only user FK index is documented | DB | Explain plan/volume justifies `(user_id, diagnosed_at, result_id)` index |
| Improve provider resilience/caching | Environment providers have timeouts but no retry/cache | Freshness and failure policy unspecified | Environment/ops | Cache TTL, retry/circuit policy, and stale-data UI contract agreed |
| Archive/remove `uv dust api_2` | It is duplicate, non-built source and can confuse maintenance | Ownership/history confirmation required | Repository owner | Confirm no consumer, then delete in a separate reviewed change |

## Explicitly not completed in this audit

- No production code, DB schema, frontend code, secrets, or external account state was changed.
- Passing mock tests were not treated as live API success.
- The successful build does not prove JWT, live DB, provider, or OpenAI E2E behavior because their conditional tests/integrations did not run.
