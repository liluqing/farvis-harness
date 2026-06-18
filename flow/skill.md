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

| 用户输入 | 前置条件 | 进入阶段 | 操作 |
|------|:--|:--:|------|
| 「我想做一个 XX」「帮我分析 XX 需求」 | 无 | Phase 1 | 加载 `phase-1-product-prototype/flow.md` |
| 「XX 已确认，设计下架构」 | PRD 已存在 | Phase 2 | 加载 `phase-2-architecture/flow.md` |
| 「开发 XX」「实现 XX 接口」 | PRD 已存在 | Phase 3 | 加载 `phase-3-spec-dev/flow.md` + `rules/decision-boundary.md` |
| 「开发 XX」（但 PRD 不存在） | 无 | Phase 1 | **自动降级** → 先走 Phase 1 |
| 「做集成测试」「验证 XX」 | 代码已存在 | Phase 4 | 加载 `phase-4-integration-test/flow.md` |
| 「继续做 XX」 | 状态文件存在 | 断点续接 | 读状态文件 → 从断点继续 |

### 路由规则

1. **Phase 1 无前置条件** — 任何模糊需求都从这里开始
2. **Phase 2 可选** — Phase 1 完成后，Agent 根据模块数量自动建议是否需要。简单迭代跳过（Phase 3 有 fallback）
3. **Phase 3 有前置条件** — 必须存在 PRD。无 PRD → 自动降级 Phase 1
4. **Phase 4 有前置条件** — 所有 Phase 3 任务完成 + 自验通过

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
4. **状态不丢** — 每完成阶段/切片立即更新 `shared/state.json`

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
