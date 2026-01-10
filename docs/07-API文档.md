# API 文档（面向前端）

## 0. 约定
- Base URL：`http://<host>:<port>`
- JSON：UTF-8
- 时间：ISO-8601 字符串（示例：`2026-01-10T12:00:00Z`）
- `term`：`yyyy-mm-dd-1/2`（示例：`2026-02-23-1`）
- 命名：
  - `userId`：内部 userId（数字主键）
  - `id`：账号 id（学号/工号，字符串）

## 1. 鉴权
### 1.1 Bearer Token
- access token 放在请求头：
  - `Authorization: Bearer <access_token>`
- access 过期：用 refresh 换新 token

### 1.2 Token 时长
- access：6h
- refresh：12h（落库，可撤销，多设备并存）

### 1.3 关键接口
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /auth/me`
- `POST /users/{userId}/password:reset`（上级重置登录凭证）

### 1.4 登录入参
- 登录使用前端计算的 `clientSalt/clientHash`，不传明文密码
- 登录账号输入：`userType + id`（id=学号/工号）

## 2. 分页
### 2.1 请求参数
- `page`：从 1 开始，默认 1
- `size`：默认 20，建议前端最大不超过 200

### 2.2 返回结构（统一）
```json
{
  "page": 1,
  "size": 20,
  "total": 123,
  "items": []
}
```

## 3. 错误返回与错误码
### 3.1 统一错误结构
```json
{
  "code": "VALIDATION_ERROR",
  "message": "field 'id' is required",
  "requestId": "optional-trace-id",
  "details": {
    "field": "id"
  }
}
```

### 3.2 错误码列表（与 OpenAPI 对齐）
- `AUTH_INVALID_CREDENTIALS`：账号/凭证错误
- `AUTH_TOKEN_EXPIRED`：token 过期
- `AUTH_TOKEN_REVOKED`：refresh 已撤销
- `AUTH_FORBIDDEN`：无权限
- `VALIDATION_ERROR`：参数校验失败
- `NOT_FOUND`：资源不存在
- `CONFLICT`：唯一键冲突等
- `CAPACITY_FULL`：活动容量已满（实现中使用 `CONFLICT` 统一表示容量冲突）
- `CSV_PARSE_ERROR`：CSV 解析失败（实现中使用 `VALIDATION_ERROR` 统一表示解析失败/参数错误）

## 4. CSV 导入（users:import）
### 4.1 CSV 格式
- 逗号分隔 UTF-8，可包含表头
- 字段：`id,name,collegeName`

### 4.2 导入语义
- 账号键：`(user_type, user_no)`；其中 `user_no = CSV.id`
- 学院名不存在：自动创建学院
- 账号存在：更新 `name/college`

## 5. 活动（activities）
- 报名：学生
- 审核：教师（当 requiresReview=true）
- 容量：达到 capacity 后拒绝新报名/审批（不做候补）
- 活动归属班级：`classId` 必填

## 6. 计划（plans）
- 计划归属：支持个人计划与班级计划（见 `ownerType/ownerUserId/ownerClassId`）
- 完成率：仅统计 `plan_item.status == done`
- 进度历史：通过 `GET/POST /plan-items/{itemId}/progress` 追加与查询（append-only）

## 7. OpenAPI
- 规范文件：`docs/openapi.yaml`
- 运行中导出（脚本）：`scripts/generate.sh` 会尝试从 `/v3/api-docs` 导出到 `docs/openapi.json`
