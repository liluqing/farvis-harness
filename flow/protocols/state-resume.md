# 断点续接协议

> **何时加载**：用户说“继续做”、新会话需要恢复，或 `state.json` 显示存在进行中的 Phase / 切片。

---

## 必读输入

- `.harness/flow/shared/state.json`
- `Docs/AI-CONTEXT.md`
- 当前迭代目录：`Docs/iterations/{迭代名}/`
- `.harness/ai-context/`
- 最近一次失败测试日志或命令输出（如有）

---

## 步骤 1：读取状态文件

从 `state.json` 提取：

- `iteration.id` / `iteration.goal` / `iteration.status`
- `phases.phaseN.status`
- `phases.phase3.current_slice`
- `slices[].status`
- `slices[].tdd_step`
- `slices[].self_repair_count`
- `slices[].escalated`

---

## 步骤 2：判断恢复点

```
if phase4.status == "in_progress":
    加载 phase-4-integration-test/flow.md
    从最近未完成的测试场景继续

elif phase3.status == "in_progress":
    加载 phase-3-spec-dev/flow.md
    找到 current_slice
    if self_repair_count > 0:
        从自修循环继续
    elif tdd_step:
        从 TDD 第 N 步继续
    else:
        从 Spec 分析继续

elif phase2.status == "in_progress":
    加载 phase-2-architecture/flow.md
    从最后未完成 STEP 继续

elif phase1.status == "in_progress":
    加载 phase-1-product-prototype/flow.md
    从 PRD 概要 / 原型 / 反馈调整等未完成处继续

else:
    所有 Phase pending → 这是新迭代，请用户描述需求
```

---

## 步骤 3：恢复上下文

按需重新加载：

- `Docs/iterations/{迭代名}/prd.md`
- `Docs/iterations/{迭代名}/tech-design.md`
- `Docs/iterations/{迭代名}/tasks.md`
- `Docs/iterations/{迭代名}/ddl-changes.md`
- `Docs/iterations/{迭代名}/api-changes.md`
- `Docs/iterations/{迭代名}/review-notes.md`
- `.harness/ai-context/*.yaml`

---

## 步骤 4：输出恢复消息

```markdown
📍 断点续接：<迭代名>

当前进度：
- Phase 1: ✅/🔄/⏸
- Phase 2: ✅/🔄/⏸/⏭
- Phase 3: 🔄（切片 M/N：<切片名>）
  - 当前 TDD 步骤：<步骤>
  - 自修次数：<N>
- Phase 4: ⏸/🔄

我将从「<恢复点>」继续。
```

---

## 更新规则

每推进一步，都要及时更新 `state.json`：

| 动作 | 必须更新 |
|------|----------|
| 进入 Phase | `phases.phaseN.status = in_progress` |
| 完成 Phase | `phases.phaseN.status = completed` |
| 跳过 Phase 2 | `phases.phase2.status = skipped` |
| 开始切片 | `current_slice` + `slices[].status = in_progress` |
| TDD 步骤变化 | `slices[].tdd_step` |
| 自修失败一次 | `self_repair_count += 1` |
| 切片完成 | `slices[].status = completed` |
| 迭代完成 | `iteration.status = completed` |

---

## 阻断条件

以下情况不要硬续：

- `state.json` 标记 completed，但代码或文档明显不存在
- 当前切片已失败且 `escalated: true`，但用户没有给新判断
- PRD / 架构文档与当前代码严重冲突
- `Docs/iterations/{迭代名}/` 缺失

命中阻断时，加载 `protocols/doc-health-check.md`，先修复状态或文档。
