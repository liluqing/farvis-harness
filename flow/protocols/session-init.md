# 会话初始化协议

> **何时加载**：每个新会话的第一件事。无论用户说什么，先恢复上下文，再响应。

---

## 输入

- `.harness/`
- `.harness/inbox/`
- `Docs/AI-CONTEXT.md`
- `.harness/ai-context/context.yaml` 或分文件 YAML
- `.harness/flow/shared/state.json`
- `Docs/iterations/`
- `Docs/archive/`

---

## 步骤 1：检查 Harness 项目

```
检查 .harness/ 是否存在
    ├── 不存在 → 标记为「非 Harness 项目」
    │   └── 用户提出开发需求时 → 提示「当前项目未初始化 Harness，是否先初始化？」
    │       ├── 用户同意 → 加载 SKILL.md（harness-java-init）
    │       └── 用户拒绝 → 无 Harness 模式执行
    └── 存在 → 进入步骤 2
```

无 Harness 模式下，不读 ai-context，不维护 state.json，不假装可以断点续接。

---

## 步骤 2：处理待处理事件

在恢复上下文前，先扫 `.harness/inbox/`：

```
扫描 .harness/inbox/*.json
    ├── 有 high priority 事件 → 加载 protocols/inbox-events.md，逐条处理
    │   └── 处理完后，更新 state.json（如 branch-created → 写入新迭代状态）
    ├── 有 normal/low 事件 → 输出数量，不阻塞，用户可选择立即处理或延后
    └── 无事件 → 进入步骤 3
```

## 步骤 3：恢复上下文

按顺序加载，除状态摘要外不要把全文输出给用户：

1. `Docs/AI-CONTEXT.md`：项目全局概览、当前状态摘要、索引
2. `.harness/ai-context/context.yaml` 或分文件：
   - `project-map.yaml`
   - `business-rules.yaml`
   - `error-catalog.yaml`
   - `coding-rules.yaml`
3. `.harness/flow/shared/state.json`：当前 Phase、任务、切片、TDD 步骤
4. `Docs/iterations/`：活跃迭代列表
5. 最近的迭代 PRD：优先活跃迭代，其次最近归档

---

## 步骤 4：输出状态摘要

当用户没有直接给出具体需求时，输出：

```markdown
📋 项目状态：

项目：<项目名>（<技术栈>）
当前迭代：<迭代名或无>
进度：
- Phase 1: ✅/🔄/⏸
- Phase 2: ✅/🔄/⏸/⏭ 跳过
- Phase 3: ✅/🔄/⏸（切片 M/N：<切片名>）
- Phase 4: ✅/🔄/⏸

待处理事件：<无 / N 个 high priority>

等待你的指令。可以说：
- 「继续做 XX」→ 从断点继续
- 「开始新迭代：XX」→ 启动新迭代
- 「项目状态」→ 查看详细进度
```

如果用户本轮已经给了明确开发需求，跳过长摘要，直接进入需求规模评估。

---

## 特殊情况

| 情况 | 处理 |
|------|------|
| `state.json` 不存在 | 输出「项目已初始化 Harness，但尚无迭代记录。请描述你的需求。」 |
| `state.json` 存在但所有 Phase 都是 pending | 同上 |
| 所有迭代 completed | 输出「所有迭代已完成。可以开始新迭代或查看历史。」 |
| inbox 有未处理事件（步骤 2 未处理） | 提示用户「有 N 个待处理事件，是否先处理？」 |
| `Docs/AI-CONTEXT.md` 缺失 | 从 `Docs/project/` 和 `.harness/ai-context/` 恢复最小摘要，并建议执行同步 |

---

## 下一步加载

| 用户输入 / 状态 | 下一步 |
|----------------|--------|
| 继续 / 恢复 | `protocols/state-resume.md` |
| 新需求 / 变更 | `routes/demand-sizing.md` |
| inbox 事件 | `protocols/inbox-events.md` |
| Phase 入口不明确 | `routes/phase-routing.md` |
| 只问问题 | 直接回答，不进入 Phase |
