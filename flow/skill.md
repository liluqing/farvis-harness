---
name: harness-dev-flow
description: Harness 人机协作开发流程——从需求到交付的迭代式闭环。人定义目标和关键决策，Harness 控制流程和规范，Agent 负责实际执行。
version: 4.0.0
tags: [harness, development, flow, tdd, product, prototype, architecture, integration]
triggers:
  - 需求分析
  - 分析.*需求
  - 产品设计
  - PRD
  - 原型
  - 我想做
  - 帮我设计
  - 实现.*接口
  - 写.*代码
  - 集成测试
  - 验证.*功能
---

# Harness 人机协作开发流程

当用户提出软件开发相关需求时，加载此 Skill。

## 核心定位

这是一套**人机协作的迭代式开发流程**。人定义目标，Harness 控制节奏和规范，Agent 负责实际执行。

**不是全自动编排引擎。** 当前阶段用方法论指导人工编排——通过实践摸清卡点后再考虑自动化。

**核心原则：让 Agent 尽量自己往下跑，不要动不动停下来问人。**

---

## 人机协作边界

```
人定义迭代目标（需求描述）
    ↓
Agent 拆解任务切片 → 人快速 review 确认
    ↓
Agent 逐切片开发（读上下文 → 按模板 → 写代码 → 跑测试）
    ↓
测试通过？→ 自动推进下一切片
测试失败？→ Agent 自己分析日志 → 修复 → 重跑
    ↓
连续重试修不好？→ 找人介入（这是真正的卡点）
    ↓
全部切片完成 → Agent 自动跑集成验证 → 输出报告
    ↓
人 review 最终产出
```

人只介入 **3 个关键点**：定义目标、拆解确认、卡点排障 + 最终验收。

---

## 迭代模型

开发以**迭代**为单位推进。每个迭代是一个完整的闭环：

```
迭代 N
├── ① 需求确认（Phase 1）
│     人定义目标 → Agent 分析需求 → 产出 PRD 概要 → 人确认
├── ② 架构设计（Phase 2，可选）
│     多模块/新系统时走，简单迭代可跳过
├── ③ 逐切片开发（Phase 3）
│     Agent 拆切片 → 人确认 → Agent 逐个 TDD 开发 → 自验
├── ④ 集成验证（Phase 4）
│     全部切片完成 → Agent 跑集成测试 → 输出报告
└── 交付
      人 review → 确认交付 → 迭代 N 结束 → 启动迭代 N+1
```

**第一个迭代**走完整流程（Phase 1 产出完整 PRD）。
**后续迭代** Phase 1 可以简化走（增量需求确认，不重新做完整 PRD）。

---

## 阶段路由

根据用户输入判断进入哪个阶段：

| 用户输入 | 前置条件 | 进入阶段 | 加载文件 |
|------|:--|:--:|------|
| 「我想做一个 XX」「帮我分析 XX 需求」 | 无 | Phase 1 | `phase-1-product-prototype/flow.md` |
| 「XX 已确认，设计下架构」 | PRD 已存在 | Phase 2 | `phase-2-architecture/flow.md` |
| 「开发 XX」「实现 XX 接口」 | PRD 已存在 | Phase 3 | `phase-3-spec-dev/flow.md` + `rules/decision-boundary.md` |
| 「开发 XX」（但 PRD 不存在） | 无 | Phase 1 | **自动降级** → 先走 Phase 1 |
| 「做集成测试」「验证 XX」 | 代码已存在 | Phase 4 | `phase-4-integration-test/flow.md` |
| 「继续做 XX」 | 状态文件存在 | 断点续接 | 见下方「断点续接协议」 |

### 路由规则

1. **Phase 1 无前置条件** — 任何模糊需求都从这里开始
2. **Phase 2 可选** — Phase 1 完成后，Agent 根据模块数量自动建议是否需要。简单迭代跳过（Phase 3 有 fallback）
3. **Phase 3 有前置条件** — 必须存在 PRD。无 PRD → 自动降级 Phase 1
4. **Phase 4 有前置条件** — 所有 Phase 3 任务完成 + 自验通过

### 自动路由

如果用户没说清楚从哪个 Phase 开始，Agent 根据项目状态推断：
- 没有 PRD → Phase 1
- 有 PRD 但没有架构文档 → Phase 2
- 有架构文档但没有代码 → Phase 3
- 有代码但没有集成测试 → Phase 4

### 断点续接协议

用户说「继续做 XX」时，Agent 按以下协议恢复：

**步骤 1：读状态文件**

读取 `.harness/flow/shared/state.json`，提取：
- `iteration.id` → 当前迭代号
- `phases.phaseN.status` → 各阶段状态（pending/in_progress/completed/skipped）
- `phases.phase3.current_slice` → 当前切片 ID
- `slices[].status` → 各切片状态（pending/in_progress/completed/failed）
- `slices[].tdd_step` → 当前切片做到 TDD 第几步（①~⑤）
- `slices[].self_repair_count` → 自修次数

**步骤 2：判断从哪个 Phase 继续**

```
if phases.phase4.status == "in_progress":
    加载 phase-4-integration-test/flow.md
    从 STEP 3（执行测试）继续，跳过已执行的场景
    
elif phases.phase3.status == "in_progress":
    加载 phase-3-spec-dev/flow.md
    current_slice = state.slices.find(id == state.phases.phase3.current_slice)
    
    if current_slice.self_repair_count > 0:
        从「自修循环」继续（分析日志→修复→重跑）
    elif current_slice.tdd_step:
        从 TDD 第 N 步继续（如 ② 单元测试 RED 后继续实现）
    else:
        从 Spec 分析开始
        跳过已 completed 的切片
    
elif phases.phase2.status == "in_progress":
    加载 phase-2-architecture/flow.md
    从最后完成的 STEP 继续
    
elif phases.phase1.status == "in_progress":
    加载 phase-1-product-prototype/flow.md
    从最后完成的 STEP 继续
    
else:
    所有 Phase 都是 pending → 提示用户「这是新迭代，请说需求」
```

**步骤 3：恢复上下文**

恢复时需要重新加载：
- PRD（`docs/product/prd-<功能名>.md`）
- 架构文档（如有）
- 已完成切片的技术设计（`docs/design/design-*.md`）
- `.harness/ai-context/` 全部上下文

**步骤 4：输出恢复消息**

```
📍 断点续接：迭代 <id>

当前进度：
- Phase 1: ✅ 完成
- Phase 2: ✅ 完成（X 个任务）
- Phase 3: 🔄 进行中（切片 M/N：<切片名>）
  - 已完成的切片：切片 1, 切片 2
  - 当前切片：<切片名>，做到 TDD ② 单元测试
- Phase 4: ⏸ 待执行

从「TDD ② 单元测试」继续。
```

---

## 跨阶段数据流

各阶段之间的数据传递关系：

```
Phase 1（需求）
  产出：PRD（docs/product/prd-<功能名>.md）
  产出：Harness 上下文（.harness/ai-context/business-rules.yaml + error-catalog.yaml）
    ↓
Phase 2（架构，可选）
  输入：PRD
  产出：架构文档（docs/architecture/architecture-<功能名>.md）
  产出：任务清单（docs/architecture/task-list-<功能名>.md）
  产出：Harness 上下文（.harness/ai-context/project-map.yaml + adr/*.md）
    ↓
Phase 3（开发）
  输入：PRD + 架构文档 + 任务清单 + Harness 上下文
  产出：源代码 + 测试
  产出：技术设计（docs/design/design-<功能名>-<切片名>.md，每切片一份）
  产出：状态文件（.harness/flow/shared/state.json）
    ↓
Phase 4（集成测试）
  输入：所有代码 + PRD + 架构文档 + Harness 上下文 + 状态文件
  产出：集成测试报告（docs/integration-test/report-<功能名>.md）
  同步：更新 error-catalog.yaml（如有新错误码）
    ↓
交付
```

**关键原则：**
- PRD 是唯一贯穿全程的文档，所有后续阶段都引用它
- Harness 上下文（`.harness/ai-context/`）是共享状态，各阶段按职责更新
- 任务清单是 Phase 2 → Phase 3 的交接物，Phase 3 按清单逐个任务拆切片

---

## 各阶段职责

| 维度 | Phase 1 | Phase 2 | Phase 3 | Phase 4 |
|------|------|------|------|------|
| **核心问题** | 做什么？ | 怎么做？ | 做出来 | 做对了吗？ |
| **人机分工** | 人定目标，Agent 辅助分析 | Agent 设计，人确认方向 | Agent 自主开发，人卡点介入 | Agent 自动验证，人验收 |
| **关键产出** | PRD + 原型 | 架构文档 + 任务清单 | 代码 + 测试 | 集成测试报告 |

---

## 关键原则

1. **先概要后详细** — 每个阶段的产出先出大纲/概要 → 确认方向 → 再展开。禁止未对齐大纲就直接产出完整文档
2. **该问问、该定定** — 三个分支：A（Agent 自定）/ B（必问用户）/ C（暂定+标记 `[待确认]`）
3. **Phase 3 自验 ≠ Phase 4 集成测试** — 自验是"我没搞坏别人"，集成测试是"整个系统端到端可工作"
- **状态跟踪：** 每完成阶段/切片立即更新 `.harness/flow/shared/state.json`
- **文档路径：** 所有产出文档都写在 `docs/` 目录下，具体路径见各 Phase flow.md

---

## 跨迭代文档关联

多次迭代间，文档通过**功能名**区分和关联：

| 文档类型 | 命名规则 | 示例 |
|---------|---------|------|
| PRD | `prd-<功能名>.md` | `prd-user-module.md` |
| 架构文档 | `architecture-<功能名>.md` | `architecture-user-module.md` |
| 任务清单 | `task-list-<功能名>.md` | `task-list-user-module.md` |
| 技术设计 | `design-<功能名>-<切片名>.md` | `design-user-create-user.md` |
| 集成测试报告 | `report-<功能名>.md` | `report-user-module.md` |

**迭代间依赖：**
- 迭代 N+1 的 Phase 3 如果需要引用迭代 N 的模块，通过 PRD 和架构文档关联
- 跨迭代的接口变更，在架构文档中明确标注"依赖迭代 N 的模块 X"

**Harness 上下文更新策略：**
- `.harness/ai-context/project-map.yaml`: 每个 Phase 产出新模块时**追加**，不覆盖
- `.harness/ai-context/business-rules.yaml`: 同上，追加模式
- `.harness/ai-context/coding-rules.yaml`: 同上
- `.harness/ai-context/error-catalog.yaml`: 同上
- `.harness/ai-context/adr/`: 每个 ADR 独立文件，天然不冲突

**冲突处理：** 如果多次迭代更新了同一个模块的规则，在 YAML 中标注版本号和迭代来源。

---

## Skill 加载机制

本 Skill（`skill.md`）是 Harness 开发流程的**总入口**，定义了 4 个 Phase 的路由。

**加载行为：**
- 用户说"开始 Harness 开发" → Agent 加载本文件
- 用户说"开始 Phase 1/2/3/4" → Agent 加载对应的 `phase-X-xxx/flow.md`
- 加载是**追加模式**：保留本文件的上下文（人机协作边界、迭代模型等），叠加 phase-specific 的流程定义

**上下文继承：**
- Phase-specific flow.md 可以引用本文件定义的通用概念（如"切片"、"自修循环"）
- Phase-specific flow.md 不需要重复定义人机协作边界、决策树等通用原则

---

## 项目初始化

### Java 项目

对 Agent 说 **「初始化 Java Harness」** → Agent 加载 `harness-java-init` Skill → 检测技术栈 → 创建 `.harness/` 目录 → 写 AGENTS.md / CLAUDE.md。

**Harness 前置检查**：进入任何 Phase 前，先检查 `.harness/` 是否存在。不存在 → 提示用户先初始化。

### 非 Java 项目

直接说「我想做一个 XX 功能」即可进入 Phase 1。

---

## 参考

- `references/human-ai-boundary.md` — 人机协作边界定义（三轮纠偏过程 + 人机分工表）
- `references/agent-tips.md` — 执行时的技术陷阱和踩坑笔记
