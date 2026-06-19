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

## 会话初始化协议

> ⚠️ **每个新会话的第一件事**，不管用户说什么，都先执行这个协议。

Agent 每次被唤醒时，先恢复项目上下文，再响应用户。

### 步骤 1：检查 Harness 项目

```
检查 .harness/ 是否存在
    ├── 不存在 → 标记为「非 Harness 项目」
    │   └── 用户提出开发需求时 → 提示「当前项目未初始化 Harness，是否先初始化？」
    │       ├── 用户同意 → 加载 harness-java-init Skill
    │       └── 用户拒绝 → 以无 Harness 模式执行（不读 ai-context，不做状态跟踪）
    └── 存在 → 进入步骤 2
```

### 步骤 2：恢复上下文

按顺序加载（只读，不输出给用户）：

1. `.harness/inbox/` → 待处理事件（优先处理 high priority）
2. `Docs/AI-CONTEXT.md` → 项目全局概览（摘要 + 索引）
3. `.harness/ai-context/context.yaml`（或分文件 `project-map.yaml` + `business-rules.yaml` + `error-catalog.yaml` + `coding-rules.yaml`）→ 结构化上下文
4. `.harness/flow/shared/state.json` → 当前开发进度（切片级）
5. `Docs/iterations/` → 活跃迭代列表
6. 最近的迭代 PRD（`Docs/iterations/*/prd.md` 或 `Docs/archive/*/prd.md`，取最新修改的）

### 步骤 3：输出项目状态摘要

```
📋 项目状态：

项目：<项目名>（<技术栈>）
当前迭代：<迭代号> - <功能名>
进度：
- Phase 1: ✅/🔄/⏸
- Phase 2: ✅/🔄/⏸/⏭ 跳过
- Phase 3: ✅/🔄/⏸（切片 M/N：<切片名>）
- Phase 4: ✅/🔄/⏸

等待你的指令。可以说：
- 「继续做 XX」→ 从断点继续
- 「开始新迭代：XX」→ 启动新迭代
- 「项目状态」→ 查看详细进度
```

### 特殊情况

| 情况 | 处理 |
|------|------|
| state.json 不存在 | 输出「项目已初始化 Harness，但尚无迭代记录。请描述你的需求。」 |
| state.json 存在但所有 Phase 都是 pending | 同上 |
| 所有迭代都是 completed | 输出「所有迭代已完成。可以开始新迭代或查看历史。」 |
| 用户直接说了具体需求 | 跳过摘要，直接进入需求规模评估（见下方） |

---

## 需求规模评估与四级路由

用户提出需求后，Agent 先评估规模，再决定走什么流程。**不要无脑进 Phase 1。**

### 四级分类

| 级别 | 判断标准 | 流程 | 人工介入 |
|------|---------|------|---------|
| **微型** | ≤ 2 个文件变更，≤ 30 行代码 | 直接改，不走 Phase | 无 |
| **小型** | 3+ 个文件或 30+ 行，但 ≤ 3 个切片 | **计划模式**（见下方） | 确认计划 |
| **中型** | 4~10 个切片 | 标准流程 Phase 1→(2)→3→4 | 3 个点 |
| **大型** | >10 个切片，或跨多个领域/模块 | 建议拆成多个迭代 | 拆迭代规划 |

### Agent 如何判断级别

```
1. 需求涉及哪些模块？（1 个 vs 多个）
2. 大概需要改多少文件？（估算）
3. 需要新建多少文件？（估算）
4. 有没有跨模块的接口变更？
5. 涉及多少业务规则？

微型信号：改一个字段、加一个配置、修一个 UI bug、改个文案
小型信号：加一个 CRUD 接口、加一个简单页面、加一个配置项+对应逻辑
中型信号：加一个完整模块（多接口+前端页面+数据库表）
大型信号：做一个完整子系统、跨多个领域的功能、涉及支付/权限等横切关注点
```

### 微型变更（直接改）

**适用范围：** 改个配置、修个 typo、加个字段、改个样式——十几行代码搞定的事。

**流程：**
```
用户描述变更
    ↓
Agent 评估：≤ 2 文件 / ≤ 30 行 → 确认是微型
    ↓
直接改代码 → 跑相关测试 → 通过则报告完成
    ↓
更新 state.json（如有进行中的迭代，记录为 ad-hoc 变更）
```

**不产出：** PRD、架构文档、技术设计。杀鸡不用牛刀。

### 小型需求（计划模式）

**适用范围：** 需要改 3 个以上文件或 30 行以上代码，但整体不超过 3 个切片。

**流程：**
```
用户描述需求
    ↓
Agent 评估：3+ 文件 / 30+ 行，≤ 3 切片 → 确认是小型
    ↓
Agent 输出变更计划（见下方模板）
    ↓
人确认计划
    ↓
Agent 按计划逐个切片开发 → 自验 → 报告完成
```

**变更计划模板：**
```markdown
## 变更计划：<功能名>

### 需求理解
<一句话描述要做什么>

### 影响范围
- 涉及文件：
  - `path/to/file1.java` — 修改 XX
  - `path/to/file2.java` — 新增 XX
  - `path/to/file3.java` — 修改 XX

### 实施步骤
1. <步骤 1：做什么，改哪个文件>
2. <步骤 2：做什么，改哪个文件>
3. ...

### 测试策略
- 单元测试：<覆盖哪些>
- 集成测试：<是否需要>

### 风险点
- <可能的影响，如有>
```

**计划确认后：**
- Agent 按计划逐步实施
- 每步完成后自验（跑相关测试）
- 不需要走完整 Phase 流程
- 完成后在 state.json 中记录为小型变更

### 大型需求（拆迭代）

**Agent 先做迭代规划，而不是直接进 Phase 1。**

```
用户需求
    ↓
Agent 评估：大型（>10 切片 / 跨领域）
    ↓
输出迭代规划建议：
```

**迭代规划模板：**
```markdown
## 迭代规划：<系统/功能名>

### 整体分析
<系统概述，涉及哪些领域>

### 迭代拆分建议

| 迭代 | 范围 | 核心产出 | 预计切片数 | 依赖 |
|------|------|---------|-----------|------|
| 迭代 1 | <范围> | <产出> | ~N | 无 |
| 迭代 2 | <范围> | <产出> | ~N | 迭代 1 |
| 迭代 3 | <范围> | <产出> | ~N | 迭代 1, 2 |

### 建议
<为什么这样拆，优先级理由>
```

人确认后，按迭代逐个走标准流程。

### 路由决策树

```
用户需求进入
    ↓
是开发/变更类需求吗？
    ├── 否 → 「非开发问题」路由（见阶段路由）
    └── 是 ↓
        ↓
评估规模
    ├── 微型 → 直接改
    ├── 小型 → 计划模式
    ├── 中型 → Phase 1（简化/完整取决于是否首次迭代）
    └── 大型 → 迭代规划 → 人确认 → 逐个走 Phase 1
```

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

## 文档同步机制

### 核心理念

迭代文档（`prd.md`、`tech-design.md`、`api-changes.md`、`ddl-changes.md`）必须与代码保持一致。不一致的文档比没有文档更危险——它会误导决策。

### 强制约束：Git Pre-commit Hook

Harness 项目的 `.git/hooks/pre-commit` 会检查 `.harness/docs-sync-marker.json`：

```
docs_synced: true  → 允许提交
docs_synced: false → 阻止提交，提示先同步文档
```

### Agent 工作流程

**Phase 3 每个切片完成后：**

1. 写代码 → 跑测试 → 测试通过
2. **立即同步文档**（不等、不拖、不靠记忆）：
   - 新增/修改了接口 → 更新 `api-changes.md`
   - 新增/修改了表结构 → 更新 `ddl-changes.md`
   - 接口签名或返回类型变了 → 更新 `tech-design.md`
   - 用了临时方案 → 追加到 `_meta.yaml` 的 `known_limitations`
3. 更新 `.harness/docs-sync-marker.json`：
   ```json
   {
     "docs_synced": true,
     "last_synced_at": "<当前时间>",
     "iteration_id": "<迭代 ID>",
     "phase": "phase-3",
     "slice_name": "<切片名>",
     "updated_files": ["<更新的文档路径>"]
   }
   ```
4. 提交代码（pre-commit hook 会放行）

**Phase 4 集成测试前：**

- 如果 Phase 3 期间有任何文档调整（如测试发现的问题导致设计变更），重新同步文档并设置 `docs_synced: true`

**迭代归档时：**

- `harness-archive-iteration` Skill 会自动执行文档合并（迭代文档 → 项目文档）
- 归档完成后自动设置 `docs_synced: true`

### 什么时候设置 `docs_synced: false`？

- 修改了代码但还没同步文档时
- 发现了文档与代码不一致，正在修复时
- 用户要求先提交代码、稍后补文档时（需要用户明确授权）

### 为什么需要这个机制？

| 痛点 | 场景 | 后果 |
|------|------|------|
| 文档滞后 | Phase 3 开发时改了接口但忘了更新文档 | Phase 4 集成测试时才发现不一致 |
| 归档阻塞 | 归档时发现 `tech-design.md` 和代码对不上 | 必须回头修复，浪费时间 |
| 决策误导 | 后续迭代基于过时的文档做设计 | 设计了错误的接口契约 |

**Git hook 是硬约束**——Agent 无法绕过，只能在同步文档后才能提交。

---

## 阶段路由

根据用户输入判断进入哪个阶段。

### 标准路由

| 用户输入 | 前置条件 | 进入阶段 | 加载文件 |
|------|:--|:--:|------|
| 「我想做一个 XX」「帮我分析 XX 需求」 | 无 | Phase 1 | `phase-1-product-prototype/flow.md` |
| 「XX 已确认，设计下架构」 | PRD 已存在 | Phase 2 | `phase-2-architecture/flow.md` |
| 「开发 XX」「实现 XX 接口」 | PRD 已存在 | Phase 3 | `phase-3-spec-dev/flow.md` + `rules/decision-boundary.md` |
| 「开发 XX」（但 PRD 不存在） | 无 | Phase 1 | **自动降级** → 先走 Phase 1 |
| 「做集成测试」「验证 XX」 | 代码已存在 | Phase 4 | `phase-4-integration-test/flow.md` |
| 「继续做 XX」 | 状态文件存在 | 断点续接 | 见下方「断点续接协议」 |

### 异常路由（流程跳步处理）

用户不按标准流程走时，Agent 不能懵，要有应对方案。

| 用户输入 | 问题 | 处理方式 |
|------|------|---------|
| **「照着这个做」**（给了原型/截图/竞品） | 想跳过 Phase 1 直接开发 | **从原型反推 PRD**：Agent 分析原型 → 提取功能点 → 产出 PRD 概要 → 用户确认 → 正常流程 |
| **「改一下 XX」**（已有功能的变更） | 不是新功能，是变更 | **变更模式**：读现有 PRD/架构 → 识别影响范围 → 走计划模式（小型）或标准流程（中型+） |
| **「把 JPA 换成 MyBatis」** | 技术重构，不是需求迭代 | **技术重构模式**：评估影响范围 → 产出重构计划（类似计划模式，但侧重技术风险）→ 分切片执行 |
| **「Spring Boot 怎么配置多数据源」** | 不是开发任务，是技术咨询 | **不进入 Phase**：直接回答问题，或引导到文档/示例 |
| **「帮我做一个电商平台」** | 需求太大，一句话想搞整个系统 | **迭代规划**：走大型需求路由 → 建议拆迭代 → 用户确认后再逐个走 Phase 1 |
| **「写个登录功能」**（无 PRD） | 想跳过产品定义 | **评估规模**：如果是小型走计划模式；如果是中型+则降级到 Phase 1，向用户解释为什么需要 |

#### 从原型反推 PRD（详细流程）

当用户给的是原型/截图/竞品链接，而不是文字需求时：

```
用户提供原型
    ↓
Agent 分析原型：
  - 识别页面/功能
  - 提取交互逻辑
  - 推断业务规则
    ↓
产出 PRD 概要（基于原型推断）
  - 标注 [推断] 的内容
  - 标注 [待确认] 的模糊点
    ↓
用户确认/修正
    ↓
进入正常流程（Phase 2 或 Phase 3）
```

**关键：** 不要假设原型 = 完整需求。原型可能只是 UI，缺少业务规则、异常处理、边界条件。Agent 要主动补全。

#### 变更模式（详细流程）

当用户要改已有功能时：

```
用户描述变更
    ↓
Agent 读取现有文档：
  - 找到对应迭代的 PRD（Docs/iterations/ 或 Docs/archive/ 下的 prd.md）
  - 找到对应的 tech-design.md（如有）
  - 找到对应的模块文档（Docs/project/modules/）
  - 找到对应的代码（通过 project-map.yaml）
    ↓
评估变更规模：
  - 微型 → 直接改
  - 小型 → 计划模式（变更计划模板）
  - 中型+ → 增量 PRD → 标准流程
    ↓
执行变更
    ↓
更新相关文档（PRD、架构、ai-context）
```

**关键：** 变更后必须更新文档，否则文档和代码会脱节。

#### 技术重构模式

当用户要做技术重构时：

```
用户描述重构目标
    ↓
Agent 评估：
  - 当前技术方案是什么？
  - 目标技术方案是什么？
  - 影响范围（哪些模块、多少文件）？
  - 风险点（数据迁移、兼容性、性能）？
    ↓
产出重构计划：
  - 分阶段执行（先试点再全量？先兼容再切换？）
  - 每阶段有回滚方案
  - 测试策略（重点测什么）
    ↓
用户确认
    ↓
分切片执行（类似 Phase 3，但每个切片侧重技术风险）
```

### 路由规则

1. **Phase 1 无前置条件** — 任何模糊需求都从这里开始
2. **Phase 2 可选** — Phase 1 完成后，Agent 根据以下条件自动判断是否跳过：
   - **可跳过的条件（全部满足才跳过）**：
     - 切片数 ≤ 3（小型需求）
     - 不引入新模块（仅修改已有模块或新增单模块）
     - 不涉及跨模块接口变更
     - 不涉及新技术组件引入（如新增消息队列、缓存策略等）
     - 已有架构文档覆盖当前变更范围
   - **不可跳过的条件（任一满足则必须走 Phase 2）**：
     - 引入新模块或跨模块接口变更
     - 引入新技术组件（消息队列、缓存、外部 API 等）
     - 涉及数据模型变更（新增表、改表结构）
     - 架构文档不存在或已过时
   - 跳过时 Phase 3 有 fallback：如果开发中发现需要架构决策，暂停并补走 Phase 2
3. **Phase 3 有前置条件** — 必须存在 PRD。无 PRD → 自动降级 Phase 1
4. **Phase 4 有前置条件** — 所有 Phase 3 任务完成 + 自验通过
5. **用户不按套路 → 异常路由** — 不要硬套标准流程，用上面的异常路由表处理

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

## 文档健康度检查

> ⚠️ **每个 Phase 启动前**，以及**变更模式完成后**，Agent 执行此检查。
> 目的：防止基于过时/错误的文档做决策。

### 检查项

| 检查项 | 怎么查 | 异常信号 | 处理 |
|--------|--------|---------|------|
| **PRD 时效性** | PRD 最后修改时间 vs 最近代码提交时间 | PRD 比代码旧很多 | 提示「PRD 可能需要更新，代码可能有变更未同步」 |
| **架构文档一致性** | 架构文档中的模块列表 vs `project-map.yaml` vs 实际代码目录 | 模块数量/名称不匹配 | 提示「架构文档和实际代码结构不一致，建议先对齐」 |
| **ai-context 完整性** | `ai-context/` 各文件是否为空模板 | 文件存在但内容为模板占位符 | 提示「ai-context 文件尚未填写，建议从 PRD 提取」 |
| **state.json 一致性** | state.json 中的切片状态 vs 实际代码是否存在 | 切片标记 completed 但没有对应代码 | 提示「状态文件和实际代码不一致」 |
| **ADR 有效性** | ADR 文件中的决策是否仍然适用 | ADR 引用的技术/方案已变更 | 提示「ADR-XXX 可能已废弃」 |

### 检查时机

```
Phase 1 启动 → 检查 ai-context 完整性（首次初始化）
Phase 2 启动 → 检查 PRD 时效性 + ai-context 完整性
Phase 3 启动 → 检查 PRD + 架构文档一致性 + ai-context 完整性
Phase 4 启动 → 检查 state.json 一致性 + ai-context 完整性
变更完成后   → 检查所有相关文档是否已同步更新
```

### 检查结果处理

```
所有检查通过 → 正常进入 Phase
有警告       → 输出警告列表 → 让用户选择：
               ├── 「先更新文档」→ 暂停，帮用户更新
               ├── 「继续，我知道」→ 标注 [文档可能过时] 继续
               └── 「帮我检查下哪些变了」→ Agent 对比分析后给出建议
有严重不一致 → 阻断，要求先更新文档再继续
```

### 文档同步规则

> **铁律：改完代码必须同步文档，改完文档必须同步代码。不能只改一边。**

每次变更/迭代完成后，Agent 自检：

1. **PRD 更新了吗？** — 如果变更了需求，PRD 必须同步
2. **架构文档更新了吗？** — 如果加了新模块/改了接口，架构文档必须同步
3. **ai-context 更新了吗？** — 如果加了新模块/新错误码/新业务规则，必须追加
4. **ADR 需要新增/废弃吗？** — 如果做了技术决策变更

---

## 跨阶段数据流

各阶段之间的数据传递关系：

```
Phase 1（需求）
  产出：PRD（Docs/iterations/{迭代名}/prd.md）
  产出：Harness 上下文（.harness/ai-context/business-rules.yaml + error-catalog.yaml）
    ↓
Phase 2（架构，可选）
  输入：PRD
  产出：技术设计文档（Docs/iterations/{迭代名}/tech-design.md）
  产出：任务清单（Docs/iterations/{迭代名}/tasks.md）
  产出：Harness 上下文（.harness/ai-context/project-map.yaml + adr/*.md）
    ↓
Phase 3（开发）
  输入：PRD + 技术设计 + 任务清单 + Harness 上下文
  产出：源代码 + 测试
  产出：技术设计追加（Docs/iterations/{迭代名}/tech-design.md，追加切片设计）
  产出：状态文件（.harness/flow/shared/state.json）
  产出：DDL/API 变更（Docs/iterations/{迭代名}/ddl-changes.md + api-changes.md）
    ↓
Phase 4（集成测试）
  输入：所有代码 + PRD + 技术设计 + Harness 上下文 + 状态文件
  产出：集成测试报告追加（Docs/iterations/{迭代名}/review-notes.md）
  同步：更新 error-catalog.yaml（如有新错误码）
    ↓
迭代完成 → 归档
  调用 harness-archive-iteration Skill
  合并文档到 Docs/project/ → 移动迭代到 Docs/archive/
  调用 harness-sync-context Skill → 更新 Docs/AI-CONTEXT.md
```

**关键原则：**
- PRD 是唯一贯穿全程的文档，所有后续阶段都引用它
- Harness 上下文（`.harness/ai-context/`）是共享状态，各阶段按职责更新
- 任务清单是 Phase 2 → Phase 3 的交接物，Phase 3 按清单逐个任务拆切片
- 所有迭代文档集中存放在 `Docs/iterations/{迭代名}/` 下

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

多次迭代间，文档通过**迭代目录**区分和关联：

| 文档类型 | 位置 | 示例 |
|---------|------|------|
| PRD | `Docs/iterations/{迭代名}/prd.md` | `Docs/iterations/2026-06-19_用户认证_v1.0/prd.md` |
| 技术设计 | `Docs/iterations/{迭代名}/tech-design.md` | 同上 |
| 任务清单 | `Docs/iterations/{迭代名}/tasks.md` | 同上 |
| DDL 变更 | `Docs/iterations/{迭代名}/ddl-changes.md` | 同上 |
| API 变更 | `Docs/iterations/{迭代名}/api-changes.md` | 同上 |
| 评审记录 | `Docs/iterations/{迭代名}/review-notes.md` | 同上 |

**迭代间依赖：**
- 迭代 N+1 如果需要引用迭代 N 的模块，通过 `Docs/project/modules/` 查看当前状态
- 回溯历史决策时，通过 `Docs/archive/{迭代名}/` 查看归档文档
- 跨迭代的接口变更，在 `tech-design.md` 中标注"依赖迭代 N 的模块 X"

**Harness 上下文更新策略：**
- `.harness/ai-context/project-map.yaml`: 每个 Phase 产出新模块时**追加**，不覆盖
- `.harness/ai-context/business-rules.yaml`: 同上，追加模式
- `.harness/ai-context/coding-rules.yaml`: 同上
- `.harness/ai-context/error-catalog.yaml`: 同上
- `.harness/ai-context/adr/`: 每个 ADR 独立文件，天然不冲突
- `Docs/AI-CONTEXT.md`: 归档时由同步 Skill 自动更新

**冲突处理：** 如果多次迭代更新了同一个模块的规则，在 YAML 中标注版本号和迭代来源。

---

## 跨迭代影响分析

> 迭代 N+1 改了迭代 N 的模块时，Agent 必须评估影响范围。

### 触发条件

当新迭代的需求涉及**已有模块**的修改时（不是新增模块），自动触发影响分析。

### 分析流程

```
新迭代需求涉及已有模块 X
    ↓
Agent 读取：
  - 模块 X 的架构文档
  - 模块 X 的 project-map 条目
  - 模块 X 相关的 ADR
    ↓
Agent 分析：
  - 模块 X 被哪些其他模块依赖？（从 project-map 找）
  - 改了模块 X 的接口，会影响哪些调用方？
  - 改了模块 X 的数据模型，需要数据迁移吗？
    ↓
输出影响分析报告：
```

**影响分析报告模板：**
```markdown
## 影响分析：<变更内容>

### 直接影响
- 模块 X：<改了什么>

### 间接影响
- 模块 Y 依赖模块 X 的接口 Z → 需要同步修改
- 模块 W 读取模块 X 的表 T → 需要检查数据兼容性

### 风险评估
| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| <风险 1> | 高/中/低 | 高/中/低 | <措施> |

### 建议
- 修改顺序：先改 X → 再改 Y → 最后改 W
- 测试重点：X 的接口变更 + Y 的调用适配
```

### 严重情况

| 情况 | 处理 |
|------|------|
| 影响范围可控（≤ 3 个模块） | 纳入当前迭代，按计划执行 |
| 影响范围大（> 3 个模块） | 建议拆成两个迭代：先改核心模块，再改依赖方 |
| 影响范围不确定 | 先做技术调研（一个微型切片），评估清楚再规划 |

---

## 前置条件不满足的补充处理

### Phase 4 有 failed 切片时

```
Phase 4 启动检查 → 发现有 failed 切片
    ↓
Agent 判断：
  ├── failed 切片是核心功能 → 阻断，提示「切片 X 未通过，无法进入集成测试」
  ├── failed 切片是边缘功能 → 可选：
  │   ├── 「跳过失败切片，先测其他」→ Phase 4 标注 [跳过切片 X]
  │   └── 「先修好再说」→ 回到 Phase 3 自修循环
  └── 所有切片都 failed → 阻断，要求先回到 Phase 3
```

### PRD 过时（用户口头改了需求但没更新 PRD）

```
Agent 发现 PRD 和实际执行不一致
    ↓
Agent 提示：「PRD 中写的是 A，但你现在要求的是 B。是需求变了吗？」
    ├── 是 → 更新 PRD（增量更新，标注变更点和原因）→ 继续
    └── 不是 → 以 PRD 为准，确认用户意图
```

### 架构文档和代码不一致

```
Agent 发现架构文档中的模块/接口和实际代码不匹配
    ↓
Agent 提示：「架构文档显示有模块 X，但代码里没有（或反过来）。哪个是对的？」
    ├── 文档过时 → 更新架构文档
    ├── 代码过时 → 可能漏了实现，检查任务清单
    └── 有意为之 → 在架构文档标注「已废弃」或「计划中」
```

---

## Skill 加载机制

### Skill 清单与调用关系

```
用户触发
  ↓
harness-java-init（SKILL.md）
  → 初始化项目：创建 .harness/ + Docs/ 目录结构
  ↓
harness-dev-flow（本文件）
  → Phase 1~4 路由与执行
  → 各 Phase 产出写入 Docs/iterations/{迭代名}/
  ↓
harness-java（.harness/skills/harness-java.md）
  → Phase 3 开发时加载，读取 ai-context/*.yaml 做编码决策
  ↓
迭代完成，分支合并到 main
  ↓
post-merge hook → .harness/inbox/ 写入 branch-merged 事件
  ↓
harness-archive-iteration（.harness/skills/harness-archive-iteration.md）
  → 合并文档到 Docs/project/ → 移动到 Docs/archive/
  ↓
harness-sync-context（.harness/skills/harness-sync-context.md）
  → 更新 Docs/AI-CONTEXT.md + .ai-context-sync.json
```

### 加载行为

本 Skill（`skill.md`）是 Harness 开发流程的**总入口**，定义了 4 个 Phase 的路由。

- 用户说"开始 Harness 开发" → Agent 加载本文件
- 用户说"开始 Phase 1/2/3/4" → Agent 加载对应的 `phase-X-xxx/flow.md`
- 加载是**追加模式**：保留本文件的上下文（人机协作边界、迭代模型等），叠加 phase-specific 的流程定义

### 上下文继承

- Phase-specific flow.md 可以引用本文件定义的通用概念（如"切片"、"自修循环"）
- Phase-specific flow.md 不需要重复定义人机协作边界、决策树等通用原则

### 迭代完成时的处理

迭代开发完成、分支合并到 main 后，Agent 应：
1. 检查 `.harness/inbox/` 是否有 `branch-merged` 事件
2. 执行 `required_actions` 中列出的归档流程
3. 参照 `core-design/templates/archive-checklist.md` 逐步执行

---

## 项目初始化

### Java 项目

对 Agent 说 **「初始化 Java Harness」** → Agent 加载 `harness-java-init` Skill → 检测技术栈 → 创建 `.harness/` + `Docs/` 目录 → 写 AGENTS.md / CLAUDE.md。

**Harness 前置检查**：进入任何 Phase 前，先检查 `.harness/` 是否存在。不存在 → 提示用户先初始化。

### 非 Java 项目

直接说「我想做一个 XX 功能」即可进入 Phase 1。

---

## 文档管理体系

本项目使用四层信息模型管理文档，详见 `core-design/03-systems-integration.md`：

| 层次 | 位置 | 用途 |
|------|------|------|
| AI-CONTEXT.md | `Docs/AI-CONTEXT.md` | Agent 工作记忆，摘要+索引 |
| project/ | `Docs/project/` | 项目当前状态（归档时更新） |
| ai-context/*.yaml | `.harness/ai-context/` | 结构化上下文（模块/规则/错误码） |
| iterations/ + archive/ | `Docs/iterations/` + `Docs/archive/` | 迭代历史 |

**核心原则**：
- 所有迭代产出写入 `Docs/iterations/{迭代名}/`，不散落到其他目录
- `.harness/ai-context/*.yaml` 是结构化查询源，各 Phase 按职责追加
- `Docs/AI-CONTEXT.md` 由同步 Skill 自动维护，Agent 日常主要读这个
- `.harness/inbox/` 是外部事件的异步通道，Agent 启动时优先扫描

---

## 参考

- `references/human-ai-boundary.md` — 人机协作边界定义（三轮纠偏过程 + 人机分工表）
- `references/agent-tips.md` — 执行时的技术陷阱和踩坑笔记
- `core-design/03-systems-integration.md` — 新旧体系整合规范
- `core-design/templates/archive-checklist.md` — 归档流程 Checklist
