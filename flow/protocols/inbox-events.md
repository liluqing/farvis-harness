# Inbox 事件处理协议

> **何时加载**：`.harness/inbox/` 中存在事件，或会话初始化时需要扫描外部事件。

---

## 事件信箱

```
.harness/inbox/               # 待处理事件
.harness/inbox-processed/     # 已处理事件
```

事件由 Git Hook、CI 或人工外部系统写入。Agent 不在线时，事件仍保留在文件系统中，下一次会话恢复时处理。

---

## 处理顺序

1. 扫描 `.harness/inbox/*.json`
2. 按优先级排序：`high` → `normal` → `low`
3. 逐个读取事件的：
   - `type`
   - `payload`
   - `required_actions`
   - `suggested_actions`
   - `context`
4. 先执行 `required_actions`
5. 根据当前上下文判断是否执行 `suggested_actions`
6. 处理完成后，移动到 `.harness/inbox-processed/`

---

## 事件类型

| 事件类型 | 触发源 | 必须动作 |
|----------|--------|----------|
| `branch-created` | Git `post-checkout` | 创建迭代目录，初始化 `_meta.yaml` 和迭代文档 |
| `branch-merged` | Git `post-merge` | 执行归档流程，合并文档并同步 AI Context |
| `ci-failed` | CI | 分析失败原因，进入修复或提示用户 |
| `pr-review-requested` | 代码平台 | 执行代码审查 |
| `commit-pushed` | Git / CI | 低优先级，按需更新任务状态 |

---

## branch-created

输入：事件中的 `payload.branch`。

步骤：

1. 计算迭代名：默认使用分支名
2. 创建 `Docs/iterations/{迭代名}/`
3. 从 `core-design/templates/iteration/` 复制：
   - `_meta.yaml`
   - `prd.md`
   - `tech-design.md`
   - `tasks.md`
   - `ddl-changes.md`
   - `api-changes.md`
   - `review-notes.md`
4. 初始化 `_meta.yaml`：
   - `status: in_progress`
   - `branch: <branch>`
   - `start_date: <today>`
5. 更新或创建 `.harness/flow/shared/state.json`
6. 输出摘要，询问用户本次迭代目标

---

## branch-merged

输入：事件中的 `payload.branch`。

步骤：

1. 确认当前分支已合并到 main
2. 查找 `Docs/iterations/{branch}/`
3. 加载 `lifecycle/archive.md`
4. 调用 `.harness/skills/harness-archive-iteration.md`
5. 归档完成后调用 `lifecycle/sync-context.md`
6. 移动事件到 `.harness/inbox-processed/`

如果归档前置条件不满足，输出阻断原因，不移动事件，等待用户或 Agent 修复。

---

## ci-failed

步骤：

1. 读取 CI 失败摘要和日志链接
2. 判断失败归类：
   - 编译错误
   - 单元测试失败
   - 集成测试失败
   - 环境 / 依赖问题
3. 如果可本地复现，进入对应 Phase 的修复流程
4. 如果不可复现，输出需要用户提供的信息

---

## 处理完成留痕

处理完成后，将事件移动到：

```
.harness/inbox-processed/{原文件名}
```

如需要保留处理结果，可在事件 JSON 中追加：

```json
{
  "processed_at": "ISO8601",
  "processed_by": "Agent",
  "result": "completed | blocked | skipped",
  "notes": "处理摘要"
}
```
