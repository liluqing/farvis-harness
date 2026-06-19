---
name: harness-java-init
description: 初始化 Java 项目的 AI Coding Harness——自动检测技术栈，从模板仓库复制到项目 .harness/ 目录，通过 AGENTS.md / CLAUDE.md 渐进披露给 AI 编码工具。
version: 0.3.0
tags: [harness, java, init, spring-boot, infrastructure]
triggers:
  - 初始化.*Harness
  - 初始化.*harness
  - 搭建.*Harness
  - 接入.*Harness
  - harness.*init
  - java.*harness
---

# Harness Java 初始化 Skill

当用户说"给这个项目初始化 Java Harness"或"接入 Harness"时，加载此 Skill。

## 核心原则

1. **Harness 全部归入 `.harness/` 目录** — 不污染项目其他目录，不创建 `ai-context/`、`devops/` 等散落文件
2. **只动两个文件** — `AGENTS.md`（Qoder / Codex）和 `CLAUDE.md`（Claude Code），把 Harness 信息渐进披露写入
3. **不覆盖已有业务代码** — `.harness/` 是新增目录，AGENTS.md / CLAUDE.md 如果已存在则追加 Harness 段

---

## STEP 0: 定位模板仓库

默认路径：`~/docs/harness-research/farvis-harness/`

验证：
```bash
ls ~/docs/harness-research/farvis-harness/SKILL.md
```

如果不存在 → 询问用户仓库位置。

---

## STEP 1: 检测目标项目

### 1.1 技术栈检测

| 检测项 | 方法 | 结果示例 |
|--------|------|---------|
| 构建工具 | `ls pom.xml` / `ls build.gradle*` | Maven / Gradle |
| 框架 | 读构建文件中的依赖 | Spring Boot 3.x |
| ORM | 读构建文件 | JPA / MyBatis |
| 语言版本 | 读 `java.target` 或 `java.toolchain` | Java 17 |

### 1.2 检测 AGENTS.md / CLAUDE.md

```bash
ls AGENTS.md CLAUDE.md 2>/dev/null
```

记录哪些已存在（后续追加，不覆盖）。

### 1.3 输出检测报告

```markdown
【项目检测】
- 框架：Spring Boot 3.x + JPA
- 构建：Gradle 8.x
- Java：17
- 匹配 stack：spring-boot3-jpa ✅
- AGENTS.md：已存在（将追加 Harness 段）
- CLAUDE.md：不存在（将新建）
```

---

## STEP 2: 选择 stack

查看可用 stack：
```bash
ls ~/docs/harness-research/farvis-harness/stacks/
```

| 检测结果 | 匹配 stack |
|---------|-----------|
| Spring Boot + JPA | `spring-boot3-jpa` |
| 无匹配 | 选 `spring-boot3-jpa` 作为 fallback，告用户差异 |

---

## STEP 3: 复制到 `.harness/`

### 3.1 创建目录

```bash
# .harness/ 目录结构
mkdir -p <target>/.harness/{skills,hooks,ai-context,devops,infra,templates,principles,patterns,flow,inbox,inbox-processed}

# Docs/ 目录结构（文档管理体系）
mkdir -p <target>/Docs/{project/modules,iterations,archive}
```

### 3.2 复制 .harness/hooks/（Hook 脚本）

```bash
mkdir -p <target>/.harness/hooks
cp -r <repo>/.harness/hooks/* <target>/.harness/hooks/
```

### 3.3 创建软链接 — Git hooks

Git hooks 源文件在 `.harness/hooks/git/`，通过软链接接入 `.git/hooks/`：

```bash
# pre-commit：提交前跑 fastTest
ln -sf ../../.harness/hooks/git/pre-commit <target>/.git/hooks/pre-commit
chmod +x <target>/.harness/hooks/git/pre-commit

# pre-push：推送前跑全量测试
ln -sf ../../.harness/hooks/git/pre-push <target>/.git/hooks/pre-push
chmod +x <target>/.harness/hooks/git/pre-push

# commit-msg：校验提交信息格式
ln -sf ../../.harness/hooks/git/commit-msg <target>/.git/hooks/commit-msg
chmod +x <target>/.harness/hooks/git/commit-msg

# post-checkout：检测新迭代分支创建
ln -sf ../../.harness/hooks/git/post-checkout <target>/.git/hooks/post-checkout
chmod +x <target>/.harness/hooks/git/post-checkout

# post-merge：检测分支合并到 main
ln -sf ../../.harness/hooks/git/post-merge <target>/.git/hooks/post-merge
chmod +x <target>/.harness/hooks/git/post-merge
```

如果目标不是 git 仓库（无 `.git/` 目录）→ 跳过此步骤，在初始化报告中标注「Git hooks 未配置（非 git 仓库）」。

### 3.4 复制 .harness/skills/（运行时 Skill）

```bash
mkdir -p <target>/.harness/skills
cp <repo>/.harness/skills/harness-java.md <target>/.harness/skills/harness-java.md
cp <repo>/.harness/skills/harness-sync-context.md <target>/.harness/skills/harness-sync-context.md
cp <repo>/.harness/skills/harness-archive-iteration.md <target>/.harness/skills/harness-archive-iteration.md
```

### 3.5 创建软链接 — Skill 接入 AI 编码工具

Harness Skill 源文件在 `.harness/skills/`，通过软链接接入各工具的 Skill 目录：

```bash
# Claude Code
mkdir -p <target>/.claude/skills
ln -sf ../../.harness/skills/harness-java.md <target>/.claude/skills/harness-java.md

# Qoder / Codex
mkdir -p <target>/.codex/skills
ln -sf ../../.harness/skills/harness-java.md <target>/.codex/skills/harness-java.md

# 通用 AGENTS.md 兼容工具
mkdir -p <target>/.agents/skills
ln -sf ../../.harness/skills/harness-java.md <target>/.agents/skills/harness-java.md
```

工具如果已有 Skill 目录 → 追加软链接。如果没有 → 创建目录后加软链接。

### 3.6 复制 core/ai-context/（通用规则）

```bash
cp --no-clobber <repo>/core/ai-context/project-map.yaml    <target>/.harness/ai-context/
cp --no-clobber <repo>/core/ai-context/business-rules.yaml  <target>/.harness/ai-context/
cp --no-clobber <repo>/core/ai-context/error-catalog.yaml   <target>/.harness/ai-context/
cp --no-clobber <repo>/core/ai-context/coding-rules.yaml    <target>/.harness/ai-context/
# Stack 特化规则。命名：coding-rules-{stack}.yaml
#   例：spring-boot3-jpa → coding-rules-spring-boot3-jpa.yaml
cp --no-clobber <repo>/stacks/{stack}/coding-rules.yaml <target>/.harness/ai-context/coding-rules-{stack}.yaml
```

### 3.7 复制 stack/devops/

```bash
# 复制 devops 下所有文件（排除 build.gradle.kts，它作为建议配置写入 AGENTS.md）
# 注意：用 devops/* 而非 devops/，避免 .harness/devops/ 已存在时产生嵌套 .harness/devops/devops/
cp -r <repo>/stacks/{stack}/devops/* <target>/.harness/devops/
rm -f <target>/.harness/devops/build.gradle.kts
chmod +x <target>/.harness/devops/env-check.sh
chmod +x <target>/.harness/devops/env-reset.sh
```

**build.gradle.kts / pom.xml**：不复制到 `.harness/`。Harness 需要的编译配置（增量编译、fastTest task）写入 AGENTS.md / CLAUDE.md 的「建议配置」段，由人类开发者决定是否合并。

### 3.8 复制 stack/infra/

```bash
cp -r <repo>/stacks/{stack}/infra/* → <target>/.harness/infra/
```

### 3.9 复制 stack/templates/

```bash
# 注意：用 templates/* 而非 templates/，避免嵌套
cp -r <repo>/stacks/{stack}/templates/* <target>/.harness/templates/
```

### 3.10 复制 core/principles/ + core/patterns/（Agent 参考）

```bash
# 注意：用 */ 的内容复制（/*），避免嵌套
cp -r <repo>/core/principles/* <target>/.harness/principles/
cp -r <repo>/core/patterns/*   <target>/.harness/patterns/
```

### 3.11 复制 scripts/（工具脚本）

```bash
# 注意：用 scripts/* 而非 scripts/，避免嵌套
cp -r <repo>/stacks/{stack}/scripts/* <target>/.harness/scripts/
```

包含 `parameterize.py`（模板参数化），后续修改模板后可重新运行。

### 3.12 复制 flow/（开发流程定义）

```bash
cp -r <repo>/flow/* <target>/.harness/flow/
```

包含 Phase 1→4 的流程定义（flow.md + templates）、shared/（状态模板、TDD 五步）和 skill.md（流程总入口）。Agent 在项目内按流程开发时读这些文件。

### 3.13 初始化 Docs/（文档管理体系）

```bash
# 创建 AI-CONTEXT.md 模板
cp <repo>/core-design/templates/ai-context.md <target>/Docs/AI-CONTEXT.md

# 创建 .ai-context-sync.json
echo '{"last_sync": "", "sync_count": 0, "file_snapshots": {}, "last_sync_changes": []}' > <target>/Docs/.ai-context-sync.json

# 创建 project/ 文档模板
cp <repo>/core-design/templates/project/architecture.md <target>/Docs/project/architecture.md
cp <repo>/core-design/templates/project/data-model.md <target>/Docs/project/data-model.md
cp <repo>/core-design/templates/project/api-contracts.md <target>/Docs/project/api-contracts.md

# 添加 .gitkeep 保持空目录
touch <target>/Docs/iterations/.gitkeep
touch <target>/Docs/archive/.gitkeep
touch <target>/.harness/inbox/.gitkeep
touch <target>/.harness/inbox-processed/.gitkeep
```

**注意**：复制的模板文件包含占位符（如 `{项目名}`），需要在初始化报告中提示用户填写。

---

## STEP 4: 参数化模板

模板文件使用 `com.example` 作为占位包名。初始化完成后运行参数化脚本替换为实际包名。

```bash
python3 <repo>/stacks/{stack}/scripts/parameterize.py \
  --base-package <actual.package> \
  --target-dir <target>/src/

# 预览模式（先看哪些文件会被改）
python3 <repo>/stacks/{stack}/scripts/parameterize.py \
  --base-package <actual.package> \
  --target-dir <target>/src/ \
  --dry-run
```

也处理 `.harness/` 下的 YAML 文件中的包名引用。

如果项目还没有 `src/` 目录（全新项目）→ 跳过此步骤，在初始化报告中标注「模板参数化未执行（无 src/ 目录）」。

---

## STEP 5: 写 AGENTS.md + CLAUDE.md

两个文件内容相同（分别面向 Qoder/Codex 和 Claude Code）。结构：

### 5.1 如果文件不存在 → 新建

```markdown
# Project AI Context

> **Agent：开始任何工作前，先加载 Skill `.harness/skills/harness-java.md`**

## Harness

本项目使用 Java AI Coding Harness。完整定义在 `.harness/` 目录。

### 结构化上下文（Agent 必读）

在开始任何开发任务前，先读取以下文件：

- `Docs/AI-CONTEXT.md` — 项目全局概览（摘要+索引，日常主要读这个）
- `.harness/ai-context/project-map.yaml` — 模块边界与依赖
- `.harness/ai-context/business-rules.yaml` — 幂等/缓存/一致性约束
- `.harness/ai-context/error-catalog.yaml` — 错误码与修复路径
- `.harness/ai-context/coding-rules.yaml` — 编码规则

### 文档管理体系

本项目使用四层信息模型管理文档：

- `Docs/AI-CONTEXT.md` — Agent 工作记忆（摘要+索引）
- `Docs/project/` — 项目当前状态（只在迭代归档时更新）
- `Docs/iterations/` — 活跃迭代文档（进行中）
- `Docs/archive/` — 已归档迭代（只读）
- `.harness/ai-context/*.yaml` — 结构化上下文（机器友好）
- `.harness/inbox/` — 外部事件信箱（Agent 启动时优先扫描）

详见 `.harness/flow/skill.md` 中的「文档管理体系」章节。

### 开发模板

生成代码时参考 `.harness/templates/` 下的模板：
- `controller-template.java` / `service-template.java` / `repository-template.java`
- `entity-template.java` / `dto-template.java`

### 基础设施

- 切片测试配置：`.harness/infra/slice/`
- Outbox 模式：`.harness/infra/outbox/`
- 可观测性配置：`.harness/infra/observe/`

### 本地环境

```bash
cd .harness/devops
docker compose up -d   # MySQL + Redis + WireMock
```

### Git Hooks

提交前自动运行 fastTest，推送前运行全量测试。配置在 `.harness/hooks/git/`，已通过软链接接入 `.git/hooks/`。

额外的生命周期 hooks：
- `post-checkout`：检测从 main 创建新分支，写入 `.harness/inbox/` 事件
- `post-merge`：检测分支合并到 main，触发迭代归档流程

### 设计原理

- `.harness/principles/` — 五条方法论
- `.harness/patterns/` — Outbox / 幂等 / 熔断 / 切片测试 模式

### 建议构建配置

将以下合并到 `build.gradle.kts` 以启用增量编译和测试分层：

```kotlin
tasks.withType<JavaCompile>().configureEach {
    options.isIncremental = true
    options.isFork = true
    options.forkOptions.memoryMaximumSize = "2g"
}
tasks.register<Test>("fastTest") {
    filter { includeTestsMatching("*SliceTest") }
}
```
```

### 5.2 如果文件已存在 → 检查后追加 Harness 段

读取已有 AGENTS.md / CLAUDE.md：
- 如果已包含「## Harness」章节 → 跳过，不重复写入
- 如果不包含 → 在末尾追加 `## Harness` 段（与新建内容相同，但不覆盖已有内容）

---

## STEP 6: 输出初始化报告

```markdown
【Harness 初始化完成】

已创建/更新：

### .harness/ 目录（Harness 基础设施）
- .harness/skills/              → 运行时 Skill（harness-java / harness-sync-context / harness-archive-iteration）
- .harness/hooks/git/            → pre-commit / pre-push / commit-msg / post-checkout / post-merge（已软链接到 .git/hooks/）
- .harness/ai-context/           → 4 个 YAML 模板（请按项目填写）
- .harness/devops/              → Docker Compose + 配置文件 + 脚本（env-check/env-reset）+ Prometheus/Grafana + WireMock stubs
- .harness/infra/               → Slice / Outbox / Observe / Client / Idempotency / Exception / Result 代码
- .harness/templates/           → Agent 开发模板（含 Fixture/Contract 测试模板）
- .harness/scripts/             → 工具脚本（parameterize.py 模板参数化）
- .harness/principles/          → 五条方法论
- .harness/patterns/            → 设计模式
- .harness/flow/               → 开发流程定义（Phase 1→4 的 flow.md + templates + shared/）
- .harness/inbox/               → 外部事件信箱（Git Hook / CI 等异步事件）
- .harness/inbox-processed/     → 已处理事件存档

### Docs/ 目录（文档管理体系）
- Docs/AI-CONTEXT.md            → Agent 工作记忆（摘要+索引，< 2000 字）
- Docs/.ai-context-sync.json    → 同步元数据（Skill 内部用）
- Docs/project/                 → 项目当前状态（architecture / data-model / api-contracts / modules/）
- Docs/iterations/              → 活跃迭代文档（进行中）
- Docs/archive/                 → 已归档迭代（只读）

### 入口文件
- AGENTS.md                     → Qoder / Codex 渐进披露入口（已更新）
- CLAUDE.md                     → Claude Code 渐进披露入口（已更新）

下一步：
1. 编辑 .harness/ai-context/project-map.yaml  — 填写模块边界
2. 编辑 .harness/ai-context/business-rules.yaml — 填写业务约束
3. 编辑 Docs/AI-CONTEXT.md — 填写项目概览
4. 如果有 PRD，对我（Agent）说「从 PRD 提取上下文」
5. 完成后对我说「开始 Phase 1」进入 Harness 开发流程
```

---

## 关键原则

1. **Harness 全部在 `.harness/`** — 不污染项目其他目录，不创建散落的 `ai-context/`、`devops/` 文件夹
2. **文档管理在 `Docs/`** — 迭代文档集中存放，项目状态文档归档时更新
3. **只修改 AGENTS.md + CLAUDE.md** — 两个入口文件，渐进披露 Harness 结构。不改动其他项目文件
4. **已有文件不覆盖** — `.harness/` 内的 `cp -n`；AGENTS.md / CLAUDE.md 存在则追加不覆盖
5. **不强制合并构建配置** — 增量编译等配置写入 AGENTS.md 的「建议配置」段，人类开发者自主决定
6. **项目代码不动** — 不修改 `src/` 下的任何文件
