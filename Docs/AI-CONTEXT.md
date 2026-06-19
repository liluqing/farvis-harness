# AI Context

> 最后同步：2026-06-19T16:00:00+08:00
> 同步详情：.ai-context-sync.json

---

## 项目概览

- **项目名称**：Farvis-AI
- **技术栈**：Spring Boot 3.x + React + MySQL + Redis
- **核心外部依赖**：HeyGen API v3（数字人视频生成）
- **模块清单**：farvis-video / farvis-avatar / farvis-voice / farvis-credits / farvis-payment / farvis-heygen

---

## 当前状态摘要

### 已上线模块

| 模块 | 状态 | 说明 |
|------|------|------|
| （暂无） | - | 项目初始化阶段，尚无已上线模块 |

### 未开发/进行中模块

| 模块 | 状态 | 说明 |
|------|------|------|
| farvis-video | ⏳ 未开发 | 视频生成核心模块，调用 HeyGen API |
| farvis-avatar | ⏳ 未开发 | 数字人形象管理，8 个公共形象 + 数字分身 |
| farvis-voice | ⏳ 未开发 | 语音合成，5 种公共声音 + 语音克隆 |
| farvis-credits | ⏳ 未开发 | 积分体系，充值/消耗/流水 |
| farvis-payment | ⏳ 未开发 | 支付模块，套餐购买 |
| farvis-heygen | ⏳ 未开发 | HeyGen SDK 封装层 |

### 进行中迭代

（暂无）

### 关键约束

- 单租户架构，暂不支持多租户
- 国内网络优先，HeyGen API 需通过代理访问
- HeyGen API 配额：1000 次/月，需做限流和降级
- v1.0 提供 8 个公共形象 + 5 种公共声音

---

## 模块索引

| 模块 | 现状文档 | 状态 | 最后更新 |
|------|---------|------|---------|
| farvis-video | → project/modules/farvis-video.md | ⏳ 未开发 | 2026-06-19 |
| farvis-avatar | → project/modules/farvis-avatar.md | ⏳ 未开发 | 2026-06-19 |
| farvis-voice | → project/modules/farvis-voice.md | ⏳ 未开发 | 2026-06-19 |
| farvis-credits | → project/modules/farvis-credits.md | ⏳ 未开发 | 2026-06-19 |
| farvis-payment | → project/modules/farvis-payment.md | ⏳ 未开发 | 2026-06-19 |
| farvis-heygen | → project/modules/farvis-heygen.md | ⏳ 未开发 | 2026-06-19 |

---

## 迭代历史摘要

（暂无归档迭代）

---

## 同步信息

- **最后同步时间**：2026-06-19T16:00:00+08:00
- **同步触发源**：项目初始化
- **本次同步变更**：
  - 初始化 AI Context
  - 创建 6 个模块索引条目
- **详情见**：`.ai-context-sync.json`
