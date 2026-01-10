# 需求规格说明书（SRS）

## 1. 引言
### 1.1 目的
定义“班导师管理系统”后端的功能、接口、数据与约束，使前后端与后续实现可对齐。

### 1.2 术语
- `user_no`：账号编号，学生为学号，教师为工号（来自 CSV 字段 `id`）
- `user_type`：`STUDENT` / `TEACHER`
- `term`：学期标识，格式 `yyyy-mm-dd-1/2`
- `requiresReview`：活动报名是否需要审核

## 2. 总体描述
### 2.1 产品视角
- 前端（Web/小程序等）通过 REST API 访问后端。
- 后端负责鉴权、数据存储、业务规则与统计接口。

### 2.2 约束
- 账号唯一键：`(user_type, user_no)` 联合唯一。
- JWT：access=6h，refresh=12h；refresh 落库，支持撤销；允许多设备。
- 学院：除校级管理员外都必须绑定学院；CSV 导入学院不存在自动创建。
- 计划完成率：按 `plan_item.status=done`。
- 活动：支持报名/审核（可选）/容量限制；不做签到。
- CSV：UTF-8、逗号分隔、可含表头，字段 `id,name,collegeName`。

## 3. 功能需求（FR）
### FR-Auth-01 登录
- 输入：`user_type`、`user_no`、`password`、可选 `device_id`
- 输出：`access_token`、`refresh_token`、过期时间
- 失败：账号不存在/密码错误/账号禁用

### FR-Auth-02 刷新
- 输入：`refresh_token`
- 输出：新的 token 对
- 规则：旧 refresh 可选择一次性失效（本期可先“旋转并撤销旧 token”）

### FR-Auth-03 登出
- 输入：`refresh_token` 或 `device_id`
- 行为：将对应 refresh_token 记录 `revoked_at` 置值

### FR-User-01 CSV 导入用户
- 输入：CSV 文件（字段 `id,name,collegeName`）
- 行为：
  - 学院不存在则创建
  - `(user_type,user_no)` 不存在则创建用户
  - 存在则更新 `name/college`
- 输出：统计结果（created/updated/failed）与失败明细（行号、原因）

### FR-Activity-01 活动创建与发布
- 创建：设置 title、term、capacity（可空）、requiresReview、时间窗口（可空）
- 发布：状态从 DRAFT -> PUBLISHED

### FR-Activity-02 报名与审核
- 报名：
  - requiresReview=false：自动 APPROVED（但仍记录）
  - requiresReview=true：创建 APPLIED，待教师审核
- 容量：
  - 统计 APPROVED 数量；达到 capacity 后拒绝新 APPROVED（若 requiresReview=true 则 APPLIED 也拒绝）

### FR-Plan-01 计划与条目
- 计划：创建/查询/删除（删除包含条目）
- 条目：创建/更新状态/排序
- 完成率：done_count/total_count

### FR-Eval-01 评价
- 教师对学生创建评价（总分/总评）并可写入多条明细项

### FR-Report-01 报表
- 计划完成率报表（按 term、学院、班级聚合）
- 活动报名统计（按 term、活动聚合）

## 4. 非功能需求（NFR）
- NFR-01：所有接口采用 JSON；时间字段使用 ISO-8601（UTC 或带时区偏移）
- NFR-02：分页接口使用 `page/size`，默认 `page=1,size=20`，最大 size 约束（如 200）
- NFR-03：错误返回统一结构，含 `code`、`message`、`requestId`、`details`（可选）

## 5. 外部接口
详见：`docs/07-API文档.md` 与 `docs/openapi.yaml`。

