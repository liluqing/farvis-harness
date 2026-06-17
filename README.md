# Java AI Coding Harness

> 面向 Java 项目的 AI 协作开发基础设施模板仓库。
>
> 这是 Harness 的**完整模样**——不是某个项目的代码，而是所有 Java 项目都可以复用的开发基础设施定义。

---

## 0. 这是什么

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
│ A. 流程层 — AI 怎么干活                                    │
│    Phase 1→4、决策边界、状态管理（在 Harness Skill 中）      │
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

## 1. 仓库结构

```
harness-template/               # 本仓库
│
├── README.md                   # 你正在读的文件
├── VERSION                     # 版本号
├── SKILL.md                    # Harness 初始化 Skill（Agent 执行）
├── TODO.md                     # 各层完成度追踪
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

## 2. 如何使用

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
│   └── patterns/              # 设计模式
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

## 3. 五条方法论速览

详见 `core/principles/`。

| # | 方法论 | 一句话 | 检查：你的项目满足吗？ |
|:--|--------|------|------|
| 1 | 快速反馈 | 改完一行 3s 内知道能不能编译 | 是否有增量编译 + 切片测试？ |
| 2 | 上下文契约 | AI 看得懂项目结构和规则 | project-map 和 business-rules 是否可被 AI 读取？ |
| 3 | 自动验证 | AI 输出的代码受机器校验 | 幂等/降级/乱序是否有自动化测试？ |
| 4 | 环境一致性 | 本地成功 ≠ 侥幸能跑 | 本地依赖是否容器化？外部 API 是否有模拟？ |
| 5 | 可观测性 | 出问题能快速归因 | 日志是否结构化？是否有业务 Metrics？ |

---

## 4. 与技术栈的关系

Harness **原理**与技术栈无关。Harness **实现**与技术栈有关。

```
你现在用 Spring Boot → 用 stacks/spring-boot3-jpa/
以后换 Quarkus      → 创建 stacks/quarkus-panache/，core/ 不动
项目用 MyBatis      → 创建 stacks/spring-boot3-mybatis/，复用 spring-boot3-jpa 的 devops/，只替换 templates/
```

---

## 5. 版本

当前版本：**v0.3.0**（81 项全完成，八层 100%）

详见 `TODO.md`。

---

## 6. 相关仓库

| 仓库 | 关系 |
|------|------|
| `harness-dev-flow` Skill | Agent 执行 Harness 流程的 Skill（本仓库是 Skill 的数据源） |
| `harness-research` | Harness 研究文档和设计演进记录 |
