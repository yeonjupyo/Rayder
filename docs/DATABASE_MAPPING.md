# Database Mapping

## Notification settings

`NOTIFICATION_SETTING` stores one scheduled setting per user and type. Supported `noti_type` values are `UV`, `DUST`, and `ROUTINE`.

| Column | Type | Constraint | Meaning |
|---|---|---|---|
| `noti_id` | INT | PK, AUTO_INCREMENT | Setting ID |
| `user_id` | INT | FK, NOT NULL | Owner (`USER.user_id`) |
| `noti_type` | VARCHAR(20) | NOT NULL, CHECK | Scheduled notification type |
| `is_active` | TINYINT(1) | NOT NULL | Enabled state |
| `created_at` | DATETIME | NOT NULL | Creation time |
| `updated_at` | DATETIME | NOT NULL | Last update time |

`UNIQUE(user_id, noti_type)` prevents duplicate settings. The user FK uses `ON DELETE CASCADE`.

`NOTIFICATION_TIME` stores zero or more times for a scheduled setting. `UNIQUE(noti_id, alert_time)` rejects duplicate times, and its FK uses `ON DELETE CASCADE`.

`NOTIFICATION_WARNING_SETTING` stores the independent cumulative UV-exposure warning preference. It is keyed by `user_id`, has no time setting, and is not one of the three scheduled notification types.

The schema change is defined in `V3__normalize_notification_settings.sql`.

## User routines

`USER_ROUTINE` is a permanent, user-owned routine definition. It is not tied to a date.

| Column | Constraint | Meaning |
|---|---|---|
| `routine_id` | PK, AUTO_INCREMENT | Routine ID |
| `user_id` | FK, NOT NULL | Owner; `ON DELETE CASCADE` |
| `time_type` | NOT NULL, CHECK | `MORNING` or `EVENING` |
| `created_at` | NOT NULL | Creation time |
| `updated_at` | NOT NULL | Last update time |

`UNIQUE(user_id, time_type)` allows at most one routine for each time type.

`ROUTINE_ITEM` stores ordered items. `deleted_at` implements soft deletion, so deleted rows and their historical completion records remain. Active item order is validated and resequenced transactionally by the service. `is_ai_recommended` is provenance only and defaults to false; AI recommendation source data is not stored here.

`ROUTINE_ITEM_COMPLETION` stores date-specific state. `UNIQUE(item_id, completion_date)` prevents duplicate records, and completion writes use an upsert. Its item FK does not cascade because routine items are retained rather than physically deleted.

`CARE_MEMO` stores a user's date-specific personal checklist. It has a user FK, completion state, and creation/update timestamps. Memo deletion is physical; user deletion cascades memos.

The schema change is defined in `V4__normalize_user_routines.sql`. The former `USER_ROUTINE.target_date`, `USER_ROUTINE.progress_rate`, and `ROUTINE_ITEM.is_completed` columns are removed; progress is derived from date-specific completions.

## AI routine recommendation persistence

No AI recommendation table is added at this stage. There is no `AI_RECOMMENDATION`, `AI_RECOMMENDATION_ITEM`, recommendation ID, or recommendation-history relationship.

- `DIAGNOSIS_RESULT` is read only during generation. Historical rows are retained and one latest row is selected for the authenticated user; the table has no completion-status column.
- Environment values are fetched through `EnvironmentQueryService` and are not persisted by this feature. Request coordinates are not stored as user-profile data.
- RAG evidence and the OpenAI structured response remain transient.
- On explicit save, only morning/evening items use existing `USER_ROUTINE` / `ROUTINE_ITEM` rows. Status summary, reasons, skin type, diagnosis, and environment values are not stored. `ROUTINE_ITEM.is_ai_recommended = 1` records provenance only.
- Saved items use existing soft-delete/order behavior and `ROUTINE_ITEM_COMPLETION` for date-specific checks; they have no synchronization link to the earlier response.

The live MariaDB `DIAGNOSIS_RESULT` mapping is:

| Column | Type | Constraint | Meaning |
|---|---|---|---|
| `result_id` | `INT(11)` | PK, AUTO_INCREMENT | Diagnosis result ID |
| `user_id` | `INT(11)` | FK, NOT NULL | Owner (`USER.user_id`) |
| `skin_type` | `VARCHAR(20)` | NOT NULL | Skin type input |
| `result_summary` | `VARCHAR(255)` | NULL | Diagnosis result/summary input |
| `diagnosed_at` | `DATETIME` | NULL, default `CURRENT_TIMESTAMP` | Diagnosis time |

There is no completion-status or separate completion-time column, so every stored row is treated as completed. The latest result for a user is selected deterministically with `ORDER BY diagnosed_at DESC, result_id DESC LIMIT 1`. The existing indexes are the primary index on `result_id` and a non-unique FK index on `user_id`; there is currently no composite index matching the latest-result lookup. Do not add one merely for the design stage; assess it with query volume when implementing the mapper.

Existing `UNIQUE(user_id, time_type)` means an AI batch save reuses an existing morning/evening routine or creates the missing one. New items append after the active maximum `step_order`. An item is skipped when an active item in the same time section has an exactly equal `item_name`; duplicate names inside the same request are also stored once. This is service-level validation rather than a global database uniqueness constraint because historical soft-deleted rows remain and uniqueness is scoped to active items and time type.

AI save does not create completion rows. Completion starts independently per date when the existing completion endpoint upserts `ROUTINE_ITEM_COMPLETION`.
