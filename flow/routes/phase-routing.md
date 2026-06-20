# Phase 路由

> **何时加载**：需求被评估为中型，或需要从状态判断进入哪个 Phase。

---

## 标准路由

| 用户输入 | 前置条件 | 进入阶段 | 加载文件 |
|----------|----------|----------|----------|
| “我想做一个 XX” / “帮我分析需求” | 无 | Phase 1 | `phase-1-product-prototype/flow.md` |
| “设计架构” | PRD 已确认 | Phase 2 | `phase-2-architecture/flow.md` |
| “开始开发” / “实现接口” | PRD 已确认 | Phase 3 | `phase-3-spec-dev/flow.md` |
| “开发 XX”但无 PRD | 无 | Phase 1 | 自动降级 |
| “做集成测试” / “验证功能” | Phase 3 完成 | Phase 4 | `phase-4-integration-test/flow.md` |
| “继续做” | state 存在 | 断点续接 | `protocols/state-resume.md` |

---

## 自动推断

用户没说清楚时：

| 项目状态 | 推断 |
|----------|------|
| 没有 PRD | Phase 1 |
| 有 PRD，但无架构且需要全局设计 | Phase 2 |
| 有 PRD，架构可复用或 Phase 2 skipped | Phase 3 |
| Phase 3 completed | Phase 4 |
| Phase 4 completed | 交付 / 归档 / 新迭代 |

---

## Phase 2 是否跳过

### 可以跳过

全部满足才跳过：

- 切片数 ≤ 3
- 不引入新模块
- 不涉及跨模块接口变更
- 不引入新技术组件
- 已有架构文档覆盖当前变更

### 必须走

任一满足则走 Phase 2：

- 新模块或跨模块接口变更
- 新技术组件：消息队列、缓存、外部 API 等
- 数据模型变更：新增表、改表结构
- 架构文档不存在或过时
- 多服务 / 微服务 / 部署拓扑变化

跳过 Phase 2 时，Phase 3 必须执行全局设计 fallback；如果开发中发现需要架构决策，暂停并补走 Phase 2。

---

## Phase 前置条件

| Phase | 前置条件 |
|-------|----------|
| Phase 1 | 无 |
| Phase 2 | PRD 已确认 |
| Phase 3 | PRD 已确认；Phase 2 completed 或 skipped |
| Phase 4 | Phase 3 completed，所有切片 completed，自验通过 |

每个 Phase 启动前加载 `protocols/doc-health-check.md`。

---

## Phase 产出路径

| Phase | 产出 | 规范路径 |
|-------|------|----------|
| Phase 1 | PRD | `Docs/iterations/{迭代名}/prd.md` |
| Phase 1 | 业务规则 / 错误码 | `.harness/ai-context/business-rules.yaml` / `error-catalog.yaml` |
| Phase 2 | 架构与任务 | `Docs/iterations/{迭代名}/tech-design.md` / `tasks.md` |
| Phase 2 | 模块图 / ADR | `.harness/ai-context/project-map.yaml` / `adr/` |
| Phase 3 | 切片设计 | 追加到 `Docs/iterations/{迭代名}/tech-design.md` |
| Phase 3 | DDL / API 变更 | `ddl-changes.md` / `api-changes.md` |
| Phase 3 | 状态 | `.harness/flow/shared/state.json` |
| Phase 4 | 测试报告 / 评审 | `Docs/iterations/{迭代名}/review-notes.md` |

如 Phase 文件中出现旧路径，按 `core-design/03-systems-integration.md` 映射到上述路径。
