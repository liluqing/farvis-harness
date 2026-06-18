# Java AI Coding Harness

> 让 Claude Code / Codex 等 AI Coding Agent 能在 Java 项目中**自主、连续地完成开发任务**的协作基础设施。

---

## 0. 为什么存在

### 问题

我们大量使用 Claude Code、Codex 等 coding agent 做项目开发，但当前仍是**人工对话驱动**的模式——人反复给 Agent 下指令、等产出、验证、再下指令。效率极低，人被频繁打断。

### 目标

构建一套**人机协作的开发流程**：人定义目标和关键决策，Harness 通过流程 Skill 控制节奏和规范，Agent 负责实际执行。最终达到：

- **人尽可能少介入**，只在产品方向、架构选型、卡点排障时出现
- **Agent 端到端产出**验证通过、符合规范的代码
- **迭代式推进**，每个迭代从需求→开发→测试跑完一个完整闭环

### 当前阶段

**不做自动编排器。** 先用方法论指导人工编排，通过实践摸清每个环节的卡点和不确定性，让每个环节都跑顺，最后再做统一编排层。

---

## 1. 人机协作边界

核心原则：**让 Agent 尽量自己往下跑，不要动不动停下来问人。**

### 一个迭代的完整流程

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

### 人机分工表

| 环节 | 谁做 | 说明 |
|------|------|------|
| 定义迭代目标 | **人** | 产品决策，Agent 不能替 |
| 任务拆解 | **Agent 拆，人确认** | Agent 从需求拆出切片，人快速 review |
| 逐切片开发 | **Agent** | 读上下文、按模板、写代码、跑测试 |
| 测试验证 | **Agent** | 跑测试、看结果、通过就自动推进 |
| 测试失败修复 | **Agent 先自己修** | 分析日志 → 改代码 → 重跑 |
| 反复修不好 | **人介入** | 真正的卡点，需要人判断方向 |
| 集成验证 | **Agent** | 自动跑全量测试 + 输出报告 |
| 最终验收 | **人** | 看整体产出，决定是否交付 |

### 三个关键设计决策

1. **编排层暂不自动化** — 先用 Skill 指导人工编排，摸清卡点后再做编排器
2. **流程 Skill = 人的操作手册** — harness-dev-flow 不是让 Agent 自循环的引擎，而是指导人如何编排 Agent 的人机协作流程定义
3. **Agent 通过 `.harness/` 获得自主能力** — 上下文、模板、编码规则、测试基础设施，让 Agent 在每个环节有足够信息自主决策

---

## 2. 当前进度

静态基础设施层（v0.3.1）已完成：上下文 schema、代码模板、测试基础设施、Git hooks、Docker 环境。

下一步是让流程 Skill 从"Agent 自循环引擎"改为"人机协作操作手册"，然后拿一个真实迭代跑一遍验证。

**➜ 完整改造路线见 [ROADMAP.md](ROADMAP.md)**

---

## 3. 这是什么

### 一句话

**让 AI Agent 能有效开发 Java 软件的基础设施层。**

### 不是什么

| Harness 是 | Harness 不是 |
|-----------|-------------|
| 可复用的工程模板和配置 | 特定项目的业务代码 |
| 结构化知识定义（YAML schema） | 自然语言 wiki 文档 |
| 标准化工具链配置 | CI/CD 流水线定义 |
| AI 可消费的上下文格式 | Prompt 模板集合 |

### 六层组成

```
┌─────────────────────────────────────────────────────────┐
│ A. 流程层 — 人怎么编排 Agent                                 │
│    迭代流程、人机分工、决策边界（在 Harness Skill 中）          │
│    ✅ harness-dev-flow Skill                             │
├─────────────────────────────────────────────────────────┤
│ B. 上下文层 — AI 知道什么                                  │
│    project-map / business-rules / error-catalog          │
│    ✅ core/ai-context/                                   │
├─────────────────────────────────────────────────────────┤
│ C. 反馈层 — AI 改完多久知道对不对                            │
│    增量编译 / 切片测试 / 集成测试 / 契约测试                   │
│    ✅ stacks/{stack}/devops/build.gradle.kts              │
│    ✅ stacks/{stack}/infra/slice/                         │
├─────────────────────────────────────────────────────────┤
│ D. 环境层 — AI 的代码在哪里跑                               │
│    Docker Compose / WireMock / Outbox 模式                │
│    ✅ stacks/{stack}/devops/docker-compose.yml            │
│    ✅ stacks/{stack}/infra/outbox/                        │
├─────────────────────────────────────────────────────────┤
│ E. 观测层 — AI 怎么定位问题                                 │
│    结构化日志 / 业务 Metrics / Trace / Error Catalog       │
│    ✅ stacks/{stack}/infra/observe/                       │
├─────────────────────────────────────────────────────────┤
│ F. 代码模式层 — AI 按什么模板写代码                           │
│    Controller / Service / Entity / DTO / Test 模板        │
│    ✅ stacks/{stack}/templates/                           │
└─────────────────────────────────────────────────────────┘
```

---

## 4. 仓库结构

```
harness/                          # 本仓库
│
├── README.md                     # 目标定义 + 人机协作边界
├── ROADMAP.md                    # 改造路线图（当前进度、下一步）
├── VERSION                       # 版本号
├── SKILL.md                      # Harness 初始化 Skill（Agent 执行）
│
├── flow/                      # 开发流程定义（Phase 1→4）
│   ├── skill.md               # 流程总入口（Phase 路由）
│   ├── phase-1-product-prototype/
│   ├── phase-2-architecture/
│   ├── phase-3-spec-dev/
│   ├── phase-4-integration-test/
│   └── shared/                # 跨阶段共享（state 模板、TDD 五步）
│
├── core/                       # 框架无关的 Harness 原理
│   ├── principles/             # 五条方法论（必读）
│   ├── patterns/               # 通用设计模式（概念层）
│   ├── ai-context/             # 上下文结构定义（YAML schema）
│   └── devops/                 # 通用环境定义（占位）
│
├── stacks/                     # 框架特定实现（可插拔）
│   ├── spring-boot3-jpa/       # Spring Boot 3 + JPA + MySQL + Redis
│   └── ...                     # 未来：quarkus, micronaut, mybatis 等
│
└── examples/                   # 填写示例（验证模板可用性）
    └── farvis-ai/              # 数字人视频平台（Spring Boot）
```

### core/ 与 stacks/ 的分工

```
core/principles/    → "要做什么"（与技术栈无关）
core/patterns/      → "怎么做——概念上"（Outbox 是什么，不写 JPA 代码）
stacks/{stack}/     → "具体代码怎么写"（JPA @Entity + @Transactional）
```

Agent 初始化 Harness 时：
1. 读 `core/principles/` 理解五条方法论
2. 读 `core/ai-context/` 理解上下文结构
3. 按项目技术栈选 `stacks/{stack}/`
4. 复制对应模板到目标项目

---

## 5. 如何使用

### 对 Agent 说一句话

```
「给这个项目初始化 Java Harness」
```

Agent 自动：
1. 检测你的技术栈（Spring Boot / Quarkus / ...）
2. 在项目根目录创建 `.harness/`，全部文件归入其中
3. 创建/更新 `AGENTS.md`（Qoder/Codex）和 `CLAUDE.md`（Claude Code），渐进披露 Harness 结构
4. 不修改 `src/` 或已有业务代码

### 初始化后，项目结构

```
your-project/
├── .harness/                  # Harness 全部文件
│   ├── skills/                # Skill 文件（Agent 运行时加载）
│   │   └── harness-java.md
│   ├── ai-context/            # 结构化上下文（请按项目填写）
│   ├── hooks/                 # Git + AI 工具 hooks
│   │   ├── git/                #   pre-commit / pre-push / commit-msg
│   │   ├── claude/             #   Claude Code hooks（占位）
│   │   ├── codex/              #   Codex hooks（占位）
│   │   └── qoder/              #   Qoder hooks（占位）
│   ├── devops/                # Docker Compose + 构建建议
│   ├── infra/                 # Slice / Outbox / Observe 代码
│   ├── templates/             # Agent 开发模板
│   ├── principles/            # 五条方法论
│   ├── patterns/              # 设计模式
│   └── flow/                  # 开发流程定义（Phase 1→4）
│       ├── skill.md           # 流程总入口
│       ├── phase-1~4/         # 各阶段 flow.md + templates
│       └── shared/           # state 模板、TDD 五步
├── AGENTS.md                  # Qoder / Codex 入口
├── CLAUDE.md                  # Claude Code 入口
├── .claude/skills/            # Claude Code Skill 目录
│   └── harness-java.md → ../../.harness/skills/harness-java.md  (symlink)
├── .codex/skills/             # Codex Skill 目录
│   └── harness-java.md → ../../.harness/skills/harness-java.md  (symlink)
├── .agents/skills/            # AGENTS.md 兼容工具
│   └── harness-java.md → ../../.harness/skills/harness-java.md  (symlink)
├── .git/hooks/                # Git hooks（软链接到 .harness/hooks/git/）
│   ├── pre-commit → ../../.harness/hooks/git/pre-commit
│   ├── pre-push   → ../../.harness/hooks/git/pre-push
│   └── commit-msg → ../../.harness/hooks/git/commit-msg
└── src/                       # 你的业务代码（不被 Harness 修改）
```

### 随后，对 Agent 说「开始 Phase 1」

Agent 读 AGENTS.md → 发现 `.harness/ai-context/` → 加载结构化上下文 → 进入 Harness 四阶段开发流程。

---

## 6. 五条方法论速览

详见 `core/principles/`。

| # | 方法论 | 一句话 | 检查：你的项目满足吗？ |
|:--|--------|------|------|
| 1 | 快速反馈 | 改完一行 3s 内知道能不能编译 | 是否有增量编译 + 切片测试？ |
| 2 | 上下文契约 | AI 看得懂项目结构和规则 | project-map 和 business-rules 是否可被 AI 读取？ |
| 3 | 自动验证 | AI 输出的代码受机器校验 | 幂等/降级/乱序是否有自动化测试？ |
| 4 | 环境一致性 | 本地成功 ≠ 侥幸能跑 | 本地依赖是否容器化？外部 API 是否有模拟？ |
| 5 | 可观测性 | 出问题能快速归因 | 日志是否结构化？是否有业务 Metrics？ |

---

## 7. 与技术栈的关系

Harness **原理**与技术栈无关。Harness **实现**与技术栈有关。

```
你现在用 Spring Boot → 用 stacks/spring-boot3-jpa/
以后换 Quarkus      → 创建 stacks/quarkus-panache/，core/ 不动
项目用 MyBatis      → 创建 stacks/spring-boot3-mybatis/，复用 spring-boot3-jpa 的 devops/，只替换 templates/
```

---

## 8. 版本

当前版本：**v0.3.1**（静态基础设施层完成）

版本规则见 [ROADMAP.md](ROADMAP.md)。

---

## 9. 相关仓库

| 仓库 | 关系 |
|------|------|
| `harness-dev-flow` Skill | 人机协作开发流程定义——人按此编排 Agent 的迭代开发节奏 |
| `harness-research` | Harness 研究文档和设计演进记录 |
