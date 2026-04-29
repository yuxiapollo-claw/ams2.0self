# AMS 2.0 Backend API

## Response envelope

All implemented endpoints return:

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "local-dev"
}
```

Error responses keep the same top-level shape and set a non-zero `code`.

## Auth

### `POST /api/auth/login`

Request:

```json
{
  "loginName": "admin",
  "password": "admin123"
}
```

Seed data stores BCrypt password hashes; clients still submit the raw password.

Success payload:

```json
{
  "token": "session-authenticated",
  "user": {
    "id": 1,
    "loginName": "admin",
    "userName": "System Admin"
  },
  "roles": ["SYS_ADMIN"]
}
```

## Reference queries

### `GET /api/users`

Returns `data.list[]` with:

- `id`
- `userCode`
- `userName`
- `departmentName`
- `employmentStatus`
- `loginName`
- `accountStatus`

Also returns `data.total`.

### `GET /api/departments`

Returns `data.list[]` with:

- `id`
- `departmentName`
- `managerUserName`
- `status`

Also returns `data.total`.

### `GET /api/assets/tree`

Returns a nested asset tree:

- `id`
- `nodeName`
- `nodeType`
- `children[]`

### `GET /api/device-accounts/by-device?deviceNodeId=100`

Returns:

```json
{
  "list": [
    {
      "deviceNodeId": 100,
      "userName": "Zhang San",
      "accountName": "device_a_zhangsan",
      "roles": ["Operator", "Technician"]
    }
  ],
  "total": 1
}
```

### `GET /api/queries/device-permissions?deviceNodeId=100`

Frontend-compatible shape:

```json
{
  "deviceNodeId": 100,
  "roles": [
    {
      "roleName": "Operator",
      "accounts": [
        {
          "userName": "Zhang San",
          "accountName": "device_a_zhangsan"
        }
      ]
    }
  ]
}
```

### `GET /api/audit/logs`

Returns:

- `data.list[]`
  - `id`
  - `actionType`
  - `operatorName`
  - `objectType`
  - `createdAt`
- `data.total`

## Workflow

### `POST /api/requests`

Request:

```json
{
  "requestType": "ROLE_ADD",
  "targetUserId": 4,
  "targetDeviceNodeId": 100,
  "targetAccountName": "device_a_wangwu",
  "reason": "岗位调整",
  "items": [
    {
      "roleNodeId": 302
    }
  ]
}
```

Creates a persisted role-assignment request using the authenticated applicant and returns:

- `id`
- `requestNo`
- `currentStatus`
- `currentStatusLabel`
- `items[]`
  - `roleNodeId`
  - `itemStatus`

Current initial status: `WAIT_DEPT_MANAGER`.

### `POST /api/executions/{requestId}/submit`

Submits the execution step when the request is in `WAIT_QI_EXECUTE`.

Returns:

- `requestId`
- `currentStatus`

Current successful terminal status: `COMPLETED`.

## Persistence notes

- Reference data is seeded through Flyway `V2__seed_reference_data.sql`.
- Request items are persisted in `request_order_item`.
- Passwords are stored as BCrypt hashes through Flyway `V3__secure_passwords_and_request_items.sql`.
- Local default runtime uses in-memory H2 in MySQL compatibility mode; use `AMS_DATASOURCE_URL` to point at a persistent database.
- Runtime state for requests is stored in `request_order`, not an in-memory map.
- Execution completion persists role binding updates in `device_account_role`.
