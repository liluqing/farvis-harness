# 迭代归档

> **何时加载**：迭代 completed 后，或 `.harness/inbox/` 中出现 `branch-merged` 事件。

---

## 入口

归档通常由以下触发：

1. Git `post-merge` 写入 `branch-merged` 事件
2. 用户手动要求“归档迭代 XXX”
3. Agent 在 Phase 4 完成后提示用户合并并归档

详细执行规则见 `.harness/skills/harness-archive-iteration.md`。

---

## 前置条件

- 当前迭代分支已合并到 main，或用户明确要求手动归档
- `Docs/iterations/{迭代名}/` 存在
- `_meta.yaml` 中 `status: completed`
- `tasks.md` 中任务均为完成或取消
- `ddl-changes.md` / `api-changes.md` / `review-notes.md` 已更新
- 没有阻断交付的未处理问题

不满足时输出阻断清单，不移动目录。

---

## 归档动作

| 源 | 目标 | 动作 |
|----|------|------|
| `ddl-changes.md` | `Docs/project/data-model.md` | 合并表结构变化 |
| `api-changes.md` | `Docs/project/api-contracts.md` | 合并接口变化 |
| `prd.md` + `tech-design.md` | `Docs/project/modules/*.md` | 更新业务现状 |
| `tech-design.md` | `Docs/project/architecture.md` | 如有架构级变化则更新 |
| `_meta.yaml` | `_meta.yaml` | `status: archived`，写入归档信息 |
| `Docs/iterations/{迭代名}/` | `Docs/archive/{迭代名}/` | 移动目录 |

---

## 归档后

归档完成后立即加载：

- `lifecycle/sync-context.md`
- `.harness/skills/harness-sync-context.md`

同步 `Docs/AI-CONTEXT.md` 和 `Docs/.ai-context-sync.json`。

---

## 输出模板

```markdown
✅ 迭代归档完成：<迭代名>

已合并：
- DDL → Docs/project/data-model.md
- API → Docs/project/api-contracts.md
- 业务文档 → Docs/project/modules/*.md
- 架构文档 → Docs/project/architecture.md（如有）

已移动：
- Docs/iterations/<迭代名>/ → Docs/archive/<迭代名>/

已同步：
- Docs/AI-CONTEXT.md
- Docs/.ai-context-sync.json
```
