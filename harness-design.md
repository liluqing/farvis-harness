# Java Harness 设计过程记录（归档）

> ⚠️ 本文档为设计过程产物，已归档。当前 Harness 的完整定义见 [README.md](README.md)。
> 保留价值：银河技术文章分析 + Farvis-AI 参考案例。

---

## 第一部分：文章分析（银河技术）

> 原文标题：都是 AI Coding，为什么 Java 体验差了一个量级？从五条方法论构建企业级 AI Coding Harness

### 五条方法论核心

| # | 方法论 | 核心问题 | 关键手段 |
|:--|--------|---------|---------|
| 1 | 快速反馈 | 改完多久知道对不对 | 增量编译 3s + 切片测试 30s，不默认完整启动 |
| 2 | 上下文契约 | AI 是否看得懂项目 | Project Map + Business Rules + Error Catalog + Coding Rules 结构化 YAML |
| 3 | 自动验证 | AI 生成是否可信 | 四层验证（编译→单测→切片→契约），按业务风险组织 |
| 4 | 环境一致性 | 本地成功是否可迁移 | Docker Compose 标准化 + Outbox 模式 + WireMock 外部 API 模拟 |
| 5 | 可观测性 | 出问题能否快速归因 | 结构化日志 + Metrics + Tracing + Error Catalog |

### 和我们 Harness 4 阶段的对应

```
文章层             Harness 层         改造
快速反馈      →    Phase 3          build.gradle.kts + 切片测试
上下文契约    →    Phase 1 + 2       ai-context/*.yaml
自动验证      →    Phase 3 + 4       TDD 5 步 + 契约/并发回归
环境一致性    →    Phase 3          docker-compose + Outbox
可观测性      →    Phase 4 + 全局     logback + Metrics + Error Catalog
```

---

## 第二部分：参考案例 — Farvis-AI

> 完整 PRD：`~/product-docs/Farvis-AI-PRD-v1.md`

### 技术栈

- Spring Boot 3.x + Spring Data JPA + MySQL 8.0 + Redis 7.2
- 外部 API：HeyGen API v3
- 支付：支付宝 / 微信

### 模块划分（6 个）

| 模块 | 职责 | 依赖 |
|------|------|------|
| farvis-video | 视频生成、状态管理、Webhook | credits, heygen |
| farvis-avatar | 数字人管理、授权流程 | heygen, credits |
| farvis-voice | 声音管理、声音克隆 | heygen, credits |
| farvis-credits | 积分管理 | 无 |
| farvis-payment | 支付集成 | credits |
| farvis-heygen | HeyGen API 封装 | 无 |

### 10 个关键业务风险用例

| 编号 | 用例 | 验证层 |
|:--|------|:---:|
| T-01 | 32 线程同 requestId → 只生成 1 个视频，扣 1 次 Credits | 切片 |
| T-02 | Webhook 先于轮询到达 → 状态机正确 | 集成 |
| T-03 | HeyGen 5xx → 降级轮询 → 标记 failed → 退 Credits | 集成 |
| T-04 | 生成失败 → Credits 退还事务一致性 | 集成 |
| T-05 | 授权 72h 超时 → expired → 退还 | 切片 |
| T-06 | 人脸检测失败 → 退款 | 集成 |
| T-07 | 支付到账但 Credits 未到 → 补单 ≤3 次 | 集成 |
| T-08 | 重复支付幂等 | 切片 |
| T-09 | Credits 不足拦截（不调 HeyGen） | 切片 |
| T-10 | Outbox 投递失败 → 重试 → 积压告警 | 集成 |

### 填写后的 ai-context 示例

见 `examples/farvis-ai/ai-context/`：
- `project-map.yaml` — 6 模块边界 + 依赖 + 禁止项
- `business-rules.yaml` — 视频生成/数字人创建/支付等规则
- `error-catalog.yaml` — 15 个错误码（原因→修复路径→日志级别）
