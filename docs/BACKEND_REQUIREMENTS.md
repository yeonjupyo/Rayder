# Backend Requirements

## Notification phase 1

- Manage settings only; actual message delivery is out of scope.
- Scheduled types are `UV`, `DUST`, and `ROUTINE`.
- UV and dust deliver that day's peak-index information at configured fixed times.
- Routine reminders deliver at user-configured times.
- The cumulative UV-exposure warning is an independent boolean preference and cannot have times.
- A scheduled setting may contain zero or more distinct times. Empty times disable scheduling without deleting the setting.
- A setting update replaces enabled state and the complete time list in one transaction.
- Only the owner may read or mutate a setting.
- Authentication is required. The pending authentication component must expose the authenticated user ID as request attribute `authenticatedUserId`; no header, hard-coded ID, JWT, or security configuration is introduced here.
