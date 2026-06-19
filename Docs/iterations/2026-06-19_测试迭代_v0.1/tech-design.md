# 技术设计文档

## 迭代信息
- **迭代ID**: 2026-06-19_测试迭代_v0.1
- **迭代名称**: 测试迭代
- **版本**: v0.1

## 架构设计

本次迭代不涉及架构变更，仅在现有模块上新增 API 端点。

### 模块依赖关系

```
farvis-video
  ├── 依赖: farvis-heygen (调用 HeyGen API)
  └── 依赖: farvis-credits (扣减 Credits)

farvis-credits
  ├── 依赖: MySQL (存储消耗记录)
  └── 新增: 查询 API
```

## 技术决策

### 决策 1: 分辨率参数的实现方式

**选项**:
- A) 在请求体中添加 `resolution` 字段
- B) 通过 URL 查询参数传递

**决策**: 选择 A

**理由**: 分辨率是视频生成的核心配置，应该作为请求体的一部分，而非查询参数。

### 决策 2: Credits 消耗查询的性能考虑

**选项**:
- A) 直接查询 `credit_transactions` 表
- B) 新增物化视图

**决策**: 选择 A

**理由**: 当前数据量较小，直接查询性能可接受。如果后续数据量增长，再考虑物化视图。

## API 设计

### API 1: 视频生成（修改）

**端点**: `POST /api/v1/videos/generate`

**请求体**:
```json
{
  "script": "string",
  "avatar_id": "string",
  "voice_id": "string",
  "resolution": "1080p"  // 新增，可选值: "720p", "1080p"，默认 "1080p"
}
```

### API 2: Credits 消耗查询（新增）

**端点**: `GET /api/v1/credits/usage`

**查询参数**:
- `user_id` (required): 用户 ID
- `start_date` (required): 开始日期 (YYYY-MM-DD)
- `end_date` (required): 结束日期 (YYYY-MM-DD)

**响应**:
```json
{
  "total_credits": 500,
  "transactions": [
    {
      "id": "txn_001",
      "type": "video_generation",
      "credits": 50,
      "timestamp": "2026-06-19T10:30:00Z",
      "description": "生成视频 video_123"
    }
  ]
}
```

## 数据模型变更

无 DDL 变更，使用现有 `credit_transactions` 表。
