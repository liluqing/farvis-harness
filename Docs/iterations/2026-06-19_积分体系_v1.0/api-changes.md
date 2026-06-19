# API 变更

> **迭代**：2026-06-19_积分体系_v1.0
> **最后更新**：2026-06-19

---

## 新增接口

| # | 方法 | 路径 | 说明 | 调用方 |
|:--:|------|------|------|--------|
| A1 | GET | /api/v1/credits/balance | 查询当前余额 | 前端 |
| A2 | POST | /api/v1/credits/deduct | 消费扣减 | video/avatar/voice 模块 |
| A3 | GET | /api/v1/credits/transactions | 消费记录分页查询 | 前端 |
| A4 | GET | /api/v1/credits/plans | 套餐列表查询 | 前端 |
| A5 | POST | /api/v1/payment/simulate | 模拟支付（演示版） | 前端 |
| A6 | POST | /api/v1/credits/init | 新用户注册赠送 | UserService（内部） |

## 变更接口

| # | 方法 | 路径 | 变更说明 |
|:--:|------|------|---------|
| — | — | — | 本次无变更 |

---

## 接口详情

### A1: GET /api/v1/credits/balance

**响应**（200）
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "balance": 2480,
    "totalRecharged": 4000,
    "totalConsumed": 1520,
    "currentPlan": "pro"
  }
}
```

### A2: POST /api/v1/credits/deduct

**请求体**
```json
{
  "refType": "VIDEO",
  "refId": "task-123",
  "consumeType": "VIDEO"
}
```

**成功响应**（200）
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "balance": 450,
    "consumed": 50
  }
}
```

**余额不足**（402）
```json
{
  "code": 402,
  "message": "Insufficient credits. Required: 50, Current: 30",
  "data": null
}
```

### A3: GET /api/v1/credits/transactions

**参数**：`page`（默认1）, `pageSize`（默认20，最大100）

**响应**（200）
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "content": [
      {
        "id": 1,
        "type": "CONSUME",
        "amount": -50,
        "balanceAfter": 2430,
        "description": "视频生成",
        "createdAt": "2026-06-19T14:30:00"
      }
    ],
    "totalElements": 42,
    "totalPages": 3,
    "number": 0,
    "size": 20
  }
}
```

### A4: GET /api/v1/credits/plans

**响应**（200）
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {"code": "starter", "name": "入门版", "price": 99, "credits": 500, "highlighted": false},
    {"code": "pro", "name": "专业版", "price": 299, "credits": 2000, "highlighted": true},
    {"code": "enterprise", "name": "企业版", "price": 999, "credits": 10000, "highlighted": false}
  ]
}
```

### A5: POST /api/v1/payment/simulate

**请求体**
```json
{
  "planCode": "pro"
}
```

**响应**（200）
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderNo": "PAY202606191430001234",
    "status": "PAID",
    "creditsAdded": 2000,
    "newBalance": 4480
  }
}
```

### A6: POST /api/v1/credits/init

**参数**：`userId`（Long, RequestParam）

**响应**（200）
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "balance": 50,
    "totalRecharged": 50,
    "totalConsumed": 0,
    "currentPlan": null
  }
}
```

> **说明**：赠送额度硬编码为 50 Credits（演示版），不需要前端传入。
