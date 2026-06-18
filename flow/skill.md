---
name: harness-dev-flow
description: Harness 人机协作开发流程——从需求到交付的迭代式闭环。人定义目标和关键决策，Harness 控制流程和规范，Agent 负责实际执行。
version: 3.2.0
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

当用户提出软件开发相关需求时，加载此 Skill。你是 Harness 流程的执行者——**但不是全自动引擎**。

## ⚠️ 核心定位

这是一套**人机协作的迭代式开发流程方法论**。人定义目标，Harness 通过流程 Skill 控制节奏和规范，Agent 负责实际执行。

**不是全自动编排引擎。** 当前阶段用方法论指导人工编排——先通过实践摸清每个环节的卡点和不确定性，让每个环节都跑顺，最后再考虑自动化编排层。

**核心原则**：让 Agent 尽量自己往下跑，不要动不动停下来问人。

### 人机协作边界

一个迭代的完整流程：

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

人只介入 **3 个关键点**：定义目标、拆解确认、卡点排障 + 最终验收。其余环节 Agent 自主推进。

详见 `references/human-ai-boundary.md`（含三轮纠偏过程 + 人机分工表 + 三个关键设计决策）。

### 三个关键设计决策

1. **编排层暂不自动化** — 先用 Skill 指导人工编排，摸清卡点后再做编排器
2. **流程 Skill = 人的操作手册** — 不是让 Agent 自循环的引擎，而是指导人如何编排 Agent 的人机协作流程定义
3. **Agent 通过 `.harness/` 获得自主能力** — 上下文、模板、编码规则、测试基础设施，让 Agent 在每个环节有足够信息自主决策

## 四阶段总览

```
用户原始需求
    ↓
Phase 1: 产品与原型  →  产出 PRD + 可选原型 HTML
    ↓
Phase 2: 架构设计     →  产出架构文档 + 粗粒度任务清单
    ↓
Phase 3: Spec & 开发  →  逐切片 Spec 分析 + TDD 开发
    ↓
Phase 4: 集成测试     →  全量端到端验证 + 测试报告
    ↓
交付
```

## 阶段路由

根据用户输入判断进入哪个阶段：

| 用户输入 | 前置条件 | 进入阶段 | 操作 |
|------|:--|:--:|------|
| 「我想做一个 XX」「帮我分析 XX 需求」「设计 XX 功能」 | 无 | Phase 1 | 加载 `phase-1-product-prototype/flow.md` |
| 「XX 功能已确认，设计下架构」 | PRD 已存在 | Phase 2 | 加载 `phase-2-architecture/flow.md` |
| 「开发 XX」「实现 XX 接口」「写 XX 代码」 | PRD 已存在 | Phase 3 | 加载 `phase-3-spec-dev/flow.md` + `rules/decision-boundary.md` |
| 「开发 XX」（但 PRD 不存在） | 无 | Phase 1 | **自动降级** → 先走 Phase 1 产品分析 |
| 「做集成测试」「验证 XX」 | 代码已存在 | Phase 4 | 加载 `phase-4-integration-test/flow.md` |
| 「继续做 XX」 | 状态文件存在 | 断点续接 | 读状态文件 → 从断点继续 |

### 路由关键规则

1. **Phase 1 无前置条件**——任何模糊需求都从这里开始
2. **Phase 2 自动判定**——Phase 1 完成后，Agent 根据模块数量/技术选型需求自动建议是否走 Phase 2
3. **Phase 3 有前置条件**——必须存在 PRD。无 PRD → 自动降级 Phase 1
4. **Phase 3 支持「跳过 Phase 2」**——无架构文档时，Agent 在第一个切片前执行「全局设计 fallback」
5. **Phase 4 有前置条件**——所有 Phase 3 任务完成 + 自验通过

## 各阶段职责边界

| 维度 | Phase 1 | Phase 2 | Phase 3 | Phase 4 |
|------|------|------|------|------|
| **角色** | 产品经理 | 架构师 | 全栈开发者 | 测试工程师 |
| **核心问题** | 做什么？ | 怎么做？ | 做出来 | 做对了吗？ |
| **粒度** | 产品级 | 模块级（粗） | 端点/页面级（细） | 系统级（全量） |
| **关键产出** | PRD + 原型 | 架构文档 + 任务清单 | 代码 + 测试 | 集成测试报告 |
| **决策层** | 产品决策 | 架构决策 | 实现决策 | 质量判断 |

## 项目初始化

### Java 项目

对 Agent 说 **「初始化 Java Harness」** → Agent 自动加载 `harness-java-init` Skill → 检测技术栈 → 创建 `.harness/` 目录 → 写 AGENTS.md / CLAUDE.md → 通过软链接接入 Claude Code / Codex / Qoder。

**Harness 前置检查**：Agent 在进入任何 Phase 前，先检查项目根目录是否存在 `.harness/`。对于 Java 项目，如果不存在 → 提示用户先执行 `harness-java-init`。

### 非 Java 项目

对 Agent 说 **「我想做一个 XX 功能」** 即可进入 Phase 1。

## 关键原则

1. **Phase 1 先于一切** — 没有 PRD 不写代码
2. **Phase 2 建议走但不强制** — 多模块/新系统建议走，简单 CRUD 可跳过（Phase 3 有 fallback）
3. **Phase 2 的「粗」→ Phase 3 的「细」** — 一个 Phase 2 任务 = N 个 Phase 3 切片
4. **先概要后详细** — 每个阶段的产物先出大纲/概要 → 确认方向 → 再展开写详细内容。此原则适用所有技术产出（Spec、架构设计、技术分析文档、方案文档等），不限于 Phase 产物。禁止未对齐大纲就直接产出完整文档。
5. **该问问、该定定** — 三个分支：A（自定）/ B（必问）/ C（暂定+标记），每个阶段各有判断标准
6. **Phase 3 自验 ≠ Phase 4 集成测试** — 自验是「我没搞坏别人」，集成测试是「整个系统端到端可工作」
7. **状态不丢** — 每完成阶段/切片立即更新状态文件

## 技术栈实例化

当 Harness 需要适配特定技术栈（Java/Spring Boot、Python/FastAPI 等），或用户分享外部研究文章要求「结合 Harness 体系」时，走三步法：① 吸收外部研究 → 结构化 ② 读 PRD → 提取工程特征 ③ 五层注入四阶段。

**⚠️ 关键区分**：实例化的目标是搭建「Harness 本身」（空模板、配置文件、结构化约定），不是开发具体项目。参考案例 PRD 仅用于验证设计。

产出标准见 `references/tech-stack-instantiation.md`，六层模型见 `references/harness-six-layers.md`。

### 已有实例化

- **Java / Spring Boot**：模板仓库 `~/docs/harness-research/farvis-harness/`
  - 仓库结构：`core/`（框架无关原理 + 空模板）+ `stacks/spring-boot3-jpa/`（Spring Boot 特化实现）+ `examples/farvis-ai/`（填写示例）
  - 当前版本：v0.3.1（静态基础设施层完成，ROADMAP.md 定义下一步）
  - 改造路线：Step 1 流程对齐 → Step 2 实战验证 → Step 3 针对性优化

## 参考

- `references/human-ai-boundary.md` — 人机协作边界定义：三轮纠偏过程 + 人机分工表 + 三个关键设计决策
- `references/orchestration-layer-design.md` — 编排层设计分析（⚠️ 后续阶段参考，当前不做编排器）
- `references/agent-tips.md` — 执行时的技术陷阱和踩坑笔记
- `references/tech-stack-instantiation.md` — 技术栈实例化模式（通用 Harness → 特定语言/框架）
- `references/harness-six-layers.md` — Harness 六层模型（A~F）+ TODO 跟踪规范
- `references/java-harness-integration.md` — Java Harness 与 Phase 1→4 的集成细节
- `references/sub-agent-role-skills.md` — Sub-agent 角色机制
- `references/review-checklist.md` — Harness 全量交叉验证清单（发现断链/冲突/遗漏的标准流程）
- `references/v0.3.0-audit-20260618.md` — v0.3.0 开发者体验闭环审计
- `references/version-history.md` — 完整版本变更记录（v2.1.0→v3.1.0）
- `references/cron-jobs.md` — Cron job 使用参考
- `references/patch-tool-workaround.md` — patch 工具使用注意事项
- `references/patch-newline-escape-pitfall.md` — patch 换行转义陷阱

## 常见陷阱

1. **加文件 → 忘更新 init Skill**：在 `stacks/` 下新增任何文件（devops 配置、infra 类、scripts）后，必须同步更新 `harness-java-init` Skill（模板仓库 `SKILL.md`）的复制步骤。否则新文件不会被部署到目标项目。发现方法：运行 `references/review-checklist.md` Step 2。
2. **内部接口重名**：模板类（如 `ExternalApiClient`）的内部接口不要与 `infra/` 下的已有类重名。发现方法：checklist Step 3。
3. **运行时路径 ≠ 模板仓库路径**：运行时 Skill（`harness-java.md`）引用的所有路径必须反映 `.harness/` 部署后的实际结构，不能假设 build.gradle.kts 或 scripts/ 在原位置。发现方法：checklist Step 4。
4. **数据流不闭合**：Phase A 写入的数据被 Phase B 消费时，两个 Phase 的格式必须一致。最典型的断裂：Phase 1 在 business-rules.yaml 中写了 `error-scenarios`，但 Phase 4 扫描的是 `error-catalog.yaml`——两个不同的文件，格式不同。原则：**在识别信息的地方写入，在消费信息的地方读取，中间文件格式必须一致。**
5. **每波改动后跑 review**：完成一波改造后，至少跑 `references/review-checklist.md` 的 Step 2（init 完整性）和 Step 6（路径 trace）。
6. **`cp -r dir/` vs `cp -r dir/*`**：当目标目录已存在时，`cp -r src/ dst/` 会把源目录**本身**复制为 `dst/src/`（嵌套），导致 Agent 后续按原路径找模板全部 404。正确做法：`cp -r src/* dst/`（源路径以 `*` 结尾，只复制内容不复制目录壳）。
7. **`write_file` 覆盖整个文件，`patch` 做定点编辑**：对已有文件做局部修改必须用 `patch`，不要用 `write_file`。`write_file` 会覆盖整个文件。
8. **Symlink + `$0` 路径歧义**：hook 脚本通过 symlink 接入 `.git/hooks/` 时，`$0` 指向 symlink 位置，真实文件在 `.harness/hooks/git/`。hook 脚本必须用 `readlink -f "$0"` 解析真实路径后再计算 PROJECT_ROOT。
9. **方向纠偏时不要跳到相反极端**：用户纠正方向时，先分析当前方案的具体问题点，有针对性地修正。不要从"全自动"直接跳到"大量人工"。详见 `references/human-ai-boundary.md`。
