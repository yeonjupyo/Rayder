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
