# AI Context 同步

> **何时加载**：归档后、用户要求刷新 AI Context、或 Docs/project / archive 发生变化后。

---

## 目标

`Docs/AI-CONTEXT.md` 是 Agent 日常启动时的摘要和索引。它不复制全量文档，只记录足够恢复上下文的信息。

详细执行规则见 `.harness/skills/harness-sync-context.md`。

---

## 输入

- `Docs/project/`
- `Docs/archive/`
- `Docs/iterations/`
- `Docs/.ai-context-sync.json`

---

## 同步内容

| 变更来源 | 更新 AI Context |
|----------|-----------------|
| `Docs/project/architecture.md` | 当前架构摘要 |
| `Docs/project/data-model.md` | 数据模型摘要 |
| `Docs/project/api-contracts.md` | API 摘要 |
| `Docs/project/modules/*.md` | 模块索引和状态 |
| `Docs/archive/{迭代名}/_meta.yaml` | 迭代历史摘要 |
| `Docs/iterations/{迭代名}/_meta.yaml` | 进行中迭代摘要 |

---

## 同步元数据

`Docs/.ai-context-sync.json` 记录：

- `last_sync`
- `sync_count`
- `file_snapshots`
- `last_sync_changes`

如果元数据缺失或损坏，执行全量扫描重建。

---

## 输出模板

```markdown
✅ AI Context 同步完成

- 最后同步时间：<timestamp>
- 本次变更：
  - <变更 1>
  - <变更 2>
- 详情：Docs/.ai-context-sync.json
```
