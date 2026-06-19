# API 变更记录

## 迭代信息
- **迭代ID**: 2026-06-19_测试迭代_v0.1
- **迭代名称**: 测试迭代

## 变更清单

### farvis-video 模块

#### 修改 API: 视频生成

**端点**: `POST /api/v1/videos/generate`

**变更类型**: 请求体新增字段

**变更内容**:
```json
{
  "script": "string",
  "avatar_id": "string",
  "voice_id": "string",
  "resolution": "1080p"  // 新增字段
}
```

**字段说明**:
- `resolution` (string, optional): 视频分辨率，可选值 `"720p"` 或 `"1080p"`，默认 `"1080p"`

**兼容性**: 向后兼容（可选字段，有默认值）

---

### farvis-credits 模块

#### 新增 API: Credits 消耗查询

**端点**: `GET /api/v1/credits/usage`

**查询参数**:
- `user_id` (string, required): 用户 ID
- `start_date` (string, required): 开始日期，格式 `YYYY-MM-DD`
- `end_date` (string, required): 结束日期，格式 `YYYY-MM-DD`

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

**兼容性**: 新增 API，无兼容性问题
