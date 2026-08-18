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
