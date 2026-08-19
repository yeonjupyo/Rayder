# Backend Requirements

## Notification phase 1

- Manage settings only; actual message delivery is out of scope.
- Scheduled types are `UV`, `DUST`, and `ROUTINE`.
- The settings read API always returns all three scheduled types. Missing rows are represented by disabled defaults with empty times.
- Enabled scheduled settings require at least one time; disabled settings may have no times.
- UV and dust deliver that day's peak-index information at configured fixed times.
- Routine reminders deliver at user-configured times.
- The UV-risk warning is an independent boolean preference and cannot have times.
- Its threshold and cumulative-exposure criteria are system policy, not user-configurable columns.
- A scheduled setting may contain zero or more distinct times. Empty times disable scheduling without deleting the setting.
- A setting update replaces enabled state and the complete time list in one transaction.
- Only the owner may read or mutate a setting.
- Authentication is required. The pending authentication component must expose the authenticated user ID as request attribute `authenticatedUserId`; no header, hard-coded ID, JWT, or security configuration is introduced here.

## AI routine recommendation phase 1

- Read the authenticated user's latest `DIAGNOSIS_RESULT` by `diagnosed_at DESC, result_id DESC`.
- Reuse `EnvironmentQueryService`; provider failure falls back to diagnosis plus retrieved knowledge.
- Retrieve relevant chunks from `rag/Rayder_RAG.pdf` with OpenAI embeddings and in-memory cosine search.
- Generate strict structured morning/evening routines through the OpenAI Responses API.
- Keep recommendation and retrieval output transient.
- Keep generated recommendations transient; only `POST /api/routines/from-ai` explicitly converts a user's selected items into ordinary `USER_ROUTINE` and `ROUTINE_ITEM` data.
- Require `OPENAI_API_KEY` only when an AI call is made; never commit or log the secret.
