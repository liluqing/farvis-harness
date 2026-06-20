# 数据模型

> **最后更新**：2026-06-19（迭代 2026-06-19_积分体系_v1.0 归档）
> **维护规则**：仅在迭代归档时更新，迭代进行中保持不变

---

## 1. ER 关系概览

```
┌──────────┐       ┌──────────────┐       ┌──────────────┐
│   User   │ 1───1 │   Credits    │ 1───N │   Credits    │
│          │       │   Account    │       │  Transaction │
└──────────┘       └──────────────┘       └──────────────┘
     │
     │ 1
     │
     ▼ N
┌──────────────┐
│   Payment    │
│    Order     │
└──────────────┘
```

---

## 2. 表结构总览

### 2.1 credits_account（积分账户表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL, UNIQUE(uk_user_id) | 用户 ID |
| balance | INT | NOT NULL, DEFAULT 0, CHECK(>=0) | 当前余额（Credits） |
| total_recharged | INT | NOT NULL, DEFAULT 0, CHECK(>=0) | 累计充值 |
| total_consumed | INT | NOT NULL, DEFAULT 0, CHECK(>=0) | 累计消费 |
| plan_code | VARCHAR(32) | NULLABLE | 当前套餐（starter/pro/enterprise） |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL, ON UPDATE | 更新时间 |

**索引**：`uk_user_id` (user_id) — 唯一索引，保证每用户一个账户

**约束**：
- `chk_balance`: balance >= 0（余额不为负）
- `chk_recharged`: total_recharged >= 0
- `chk_consumed`: total_consumed >= 0

### 2.2 credits_transaction（积分流水表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| user_id | BIGINT | NOT NULL | 用户 ID |
| type | VARCHAR(32) | NOT NULL | 变动类型：RECHARGE/CONSUME/GIFT |
| amount | INT | NOT NULL | 变动数量（正=充值/赠送，负=消费） |
| balance_after | INT | NOT NULL | 变动后余额 |
| ref_type | VARCHAR(32) | NULLABLE | 关联业务类型：VIDEO/AVATAR/VOICE/PAYMENT/REGISTER |
| ref_id | VARCHAR(64) | NULLABLE | 关联业务 ID |
| description | VARCHAR(256) | NULLABLE | 描述 |
| created_at | DATETIME | NOT NULL | 创建时间 |

**索引**：
- `uk_ref` (ref_type, ref_id) — 唯一索引，幂等保证
- `idx_user_id_created` (user_id, created_at) — 分页查询

### 2.3 payment_order（支付订单表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| order_no | VARCHAR(64) | NOT NULL, UNIQUE | 订单号（PAY+时间戳+随机） |
| user_id | BIGINT | NOT NULL | 用户 ID |
| plan_code | VARCHAR(32) | NOT NULL | 套餐编码 |
| amount | DECIMAL(10,2) | NOT NULL | 支付金额（元） |
| credits | INT | NOT NULL | 充值 Credits 数量 |
| status | VARCHAR(16) | NOT NULL, DEFAULT 'PENDING' | 状态：PENDING/PAID/CANCELLED/FAILED |
| paid_at | DATETIME | NULLABLE | 支付完成时间 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

**索引**：
- `uk_order_no` (order_no) — 唯一索引
- `idx_user_id_status` (user_id, status)

**约束**：`chk_status`: status IN ('PENDING','PAID','CANCELLED','FAILED')

---

## 3. 变更历史

| 迭代 | 日期 | 变更类型 | 对象 | 说明 |
|------|------|---------|------|------|
| 项目初始化 | 2026-06-19 | 初始化 | - | 数据模型文档创建 |
| 2026-06-19_积分体系_v1.0 | 2026-06-19 | CREATE | credits_account | 积分账户表 |
| 2026-06-19_积分体系_v1.0 | 2026-06-19 | CREATE | credits_transaction | 积分流水表（含幂等唯一索引） |
| 2026-06-19_积分体系_v1.0 | 2026-06-19 | CREATE | payment_order | 支付订单表 |
