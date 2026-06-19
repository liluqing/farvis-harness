# AI Context

> 最后同步：2026-06-19T22:30:00+08:00
> 同步详情：.ai-context-sync.json

---

## 项目概览

- **项目名称**：Farvis-AI
- **技术栈**：Spring Boot 3.x + React + MySQL + Redis
- **核心外部依赖**：HeyGen API v3（数字人视频生成）
- **模块清单**：farvis-video / farvis-avatar / farvis-voice / farvis-credits / farvis-payment / farvis-heygen

---

## 当前状态摘要

### 已实现模块

| 模块 | 状态 | 说明 |
|------|------|------|
| farvis-credits | ✅ 已实现 | 积分体系完整闭环：赠送/充值/扣减/余额/流水/套餐，含幂等+悲观锁 |
| farvis-payment | 🔄 开发中 | 模拟支付已实现（演示版），待对接真实支付渠道 |
| farvis-video | 🔄 开发中 | 视频生成核心模块，已支持 720p/1080p 分辨率 |

### 未开发模块

| 模块 | 状态 | 说明 |
|------|------|------|
| farvis-avatar | ⏳ 未开发 | 数字人形象管理，8 个公共形象 + 数字分身 |
| farvis-voice | ⏳ 未开发 | 语音合成，5 种公共声音 + 语音克隆 |
| farvis-heygen | ⏳ 未开发 | HeyGen SDK 封装层 |

### 进行中迭代

（暂无）

### 关键约束

- 单租户架构，暂不支持多租户
- 国内网络优先，HeyGen API 需通过代理访问
- HeyGen API 配额：1000 次/月，需做限流和降级
- v1.0 提供 8 个公共形象 + 5 种公共声音
- 支付当前为模拟模式，后续需对接真实支付渠道

---

## 模块索引

| 模块 | 现状文档 | 状态 | 最后更新 |
|------|---------|------|---------|
| farvis-video | → project/modules/farvis-video.md | 🔄 开发中 | 2026-06-19 |
| farvis-avatar | → project/modules/farvis-avatar.md | ⏳ 未开发 | 2026-06-19 |
| farvis-voice | → project/modules/farvis-voice.md | ⏳ 未开发 | 2026-06-19 |
| farvis-credits | → project/modules/farvis-credits.md | ✅ 已实现 | 2026-06-19 |
| farvis-payment | → project/modules/farvis-payment.md | 🔄 开发中 | 2026-06-19 |
| farvis-heygen | → project/modules/farvis-heygen.md | ⏳ 未开发 | 2026-06-19 |

---

## 迭代历史摘要

| 迭代 | 归档位置 | 核心变更 |
|------|---------|---------|
| 2026-06-19_测试迭代_v0.1 | → archive/2026-06-19_测试迭代_v0.1/ | 验证文档管理体系流程，视频生成 API 新增 resolution 参数，Credits 消耗查询 API |
| 2026-06-19_积分体系_v1.0 | → archive/2026-06-19_积分体系_v1.0/ | 积分体系完整实现：3 张表（credits_account/credits_transaction/payment_order）、6 个 API（balance/deduct/transactions/plans/simulate/init）、前端积分中心页面（登录+余额卡片+套餐+消费记录分页），联调 5/5 场景通过 |

---

## 同步信息

- **最后同步时间**：2026-06-19T22:30:00+08:00
- **同步触发源**：归档完成（2026-06-19_积分体系_v1.0）
- **本次同步变更**：
  - 新增归档记录：2026-06-19_积分体系_v1.0
  - 更新模块状态：farvis-credits → ✅ 已实现，farvis-payment → 🔄 开发中
  - 更新 project/data-model.md：新增 3 张表结构
  - 更新 project/api-contracts.md：新增 6 个 API
  - 更新 project/modules/farvis-credits.md：完整业务规则+流程+边界
  - 更新 project/modules/farvis-payment.md：模拟支付实现
- **详情见**：`.ai-context-sync.json`
