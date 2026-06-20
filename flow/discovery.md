# 渐进式发现索引

> **用途**：让 Agent 只加载当前任务需要的流程材料，避免一次性读取完整手册。

---

## 读取原则

1. **先入口，后细节**：先读 `skill.md`，只在路由命中后加载对应协议或 Phase。
2. **先状态，后动作**：任何开发动作前，先恢复 `Docs/AI-CONTEXT.md`、`.harness/ai-context/`、`state.json` 和活跃迭代目录。
3. **按需加载**：不要为了微型修改读取 Phase 1~4 全量文档。
4. **保留上下文继承**：加载 Phase 文件时，仍继承 `skill.md` 的人机边界、状态更新和文档路径规则。
5. **读到足够就执行**：一旦具备决策所需上下文，直接推进；只有命中阻断条件才停下问人。

---

## 最小启动集

每个新会话先读：

| 顺序 | 文件 | 目的 |
|------|------|------|
| 1 | `skill.md` | 总入口、路由表、加载规则 |
| 2 | `protocols/session-init.md` | 会话初始化和上下文恢复 |
| 3 | `protocols/inbox-events.md` | 处理 Git Hook / CI 等异步事件 |
| 4 | `Docs/AI-CONTEXT.md` | 项目摘要和索引 |
| 5 | `.harness/flow/shared/state.json` | 当前 Phase、切片、TDD 进度 |

如果用户输入明确是“继续”，再读 `protocols/state-resume.md`。
如果用户输入明确是新需求或变更，再读 `routes/demand-sizing.md`。

---

## 场景到文件

| 场景 | 加载文件 |
|------|----------|
| 新会话 / 任意请求开始 | `protocols/session-init.md` |
| `.harness/inbox/` 有事件 | `protocols/inbox-events.md` |
| 用户说“继续做” | `protocols/state-resume.md` |
| 用户提出新需求 | `routes/demand-sizing.md` |
| 用户给原型 / 截图 / 竞品 | `routes/exception-routing.md#from-prototype` |
| 用户要求修改已有功能 | `routes/exception-routing.md#change-mode` |
| 用户要求技术重构 | `routes/exception-routing.md#technical-refactor` |
| 用户问技术咨询 | `routes/exception-routing.md#non-development` |
| 需要判断 Phase 入口 | `routes/phase-routing.md` |
| 每个 Phase 启动前 | `protocols/doc-health-check.md` |
| 需要理解迭代模型和数据流 | `lifecycle/iteration-model.md` |
| Phase 1 | `phase-1-product-prototype/flow.md` |
| Phase 2 | `phase-2-architecture/flow.md` |
| Phase 3 | `phase-3-spec-dev/flow.md` + `.harness/skills/harness-java.md` |
| Phase 4 | `phase-4-integration-test/flow.md` |
| 迭代完成归档 | `lifecycle/archive.md` + `.harness/skills/harness-archive-iteration.md` |
| AI Context 同步 | `lifecycle/sync-context.md` + `.harness/skills/harness-sync-context.md` |

---

## 推荐加载顺序

### 新需求

```
skill.md
  → protocols/session-init.md
  → protocols/inbox-events.md
  → routes/demand-sizing.md
  → routes/phase-routing.md（中型及以上）
  → phase-X/flow.md（按路由命中）
```

### 断点续接

```
skill.md
  → protocols/session-init.md
  → protocols/state-resume.md
  → 当前 Phase 的 flow.md
```

### 迭代归档

```
skill.md
  → protocols/session-init.md
  → protocols/inbox-events.md
  → lifecycle/archive.md
  → .harness/skills/harness-archive-iteration.md
  → lifecycle/sync-context.md
```

---

## 路径规范

当前体系以 `Docs/` 为准：

| 文档 | 规范路径 |
|------|----------|
| PRD | `Docs/iterations/{迭代名}/prd.md` |
| 技术设计 | `Docs/iterations/{迭代名}/tech-design.md` |
| 任务清单 | `Docs/iterations/{迭代名}/tasks.md` |
| DDL 变更 | `Docs/iterations/{迭代名}/ddl-changes.md` |
| API 变更 | `Docs/iterations/{迭代名}/api-changes.md` |
| 集成测试 / 评审记录 | `Docs/iterations/{迭代名}/review-notes.md` |
| 项目现状 | `Docs/project/` |
| 归档迭代 | `Docs/archive/{迭代名}/` |

**迭代目录命名规范：** `{YYYY-MM-DD}_{需求名}_{版本}`，例如 `2026-06-19_用户认证_v1.0`。目录名 = Git 分支名（一个分支一个迭代）。

如果旧 Phase 文档中出现 `docs/product/`、`docs/architecture/`、`docs/design/` 等旧路径，按 `core-design/03-systems-integration.md` 的映射转换到 `Docs/iterations/{迭代名}/`。
