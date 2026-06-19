# 示例：Farvis-AI（新体系）

本目录展示 Farvis-AI 项目使用**新文档管理体系**后的完整目录结构。

## 目录结构

```
Docs/
├── AI-CONTEXT.md                    # Agent 工作记忆（摘要+索引）
├── .ai-context-sync.json            # 同步元数据
│
├── project/                         # 项目当前状态（归档时更新）
│   ├── architecture.md              # 系统架构全景
│   ├── data-model.md                # 数据模型（当前 ER 关系）
│   ├── api-contracts.md             # 接口总览
│   └── modules/                     # 按模块拆分的业务现状
│       ├── farvis-video.md
│       ├── farvis-avatar.md
│       ├── farvis-voice.md
│       ├── farvis-credits.md
│       ├── farvis-payment.md
│       └── farvis-heygen.md
│
├── iterations/                      # 活跃迭代（进行中）
│   └── 2026-07-01_积分体系_v1.0/    # 示例：正在开发的迭代
│       ├── _meta.yaml
│       ├── prd.md
│       ├── tech-design.md
│       ├── tasks.md
│       ├── ddl-changes.md
│       ├── api-changes.md
│       └── review-notes.md
│
└── archive/                         # 已归档迭代（只读）
    └── 2026-06-19_基础框架_v0.1/    # 示例：已完成的迭代
        ├── _meta.yaml
        ├── prd.md
        ├── tech-design.md
        ├── tasks.md
        ├── ddl-changes.md
        ├── api-changes.md
        └── review-notes.md
```

## AI-CONTEXT.md 示例

```markdown
# AI Context

> 最后同步：2026-07-01T10:00:00+08:00

## 项目概览

- **项目名称**：Farvis-AI
- **技术栈**：Spring Boot 3.x + React + MySQL + Redis
- **核心外部依赖**：HeyGen API v3
- **模块清单**：farvis-video / farvis-avatar / farvis-voice / farvis-credits / farvis-payment / farvis-heygen

## 当前状态摘要

### 已上线模块

| 模块 | 状态 | 说明 |
|------|------|------|
| farvis-video | ✅ 已上线 | 基础视频生成，支持 720p/1080p |
| farvis-avatar | ✅ 已上线 | 8 个公共形象 + 数字分身 |
| farvis-voice | ✅ 已上线 | 5 种公共声音 + 语音克隆 |

### 进行中迭代

| 迭代 | 预计完成 | 核心目标 |
|------|---------|---------|
| 2026-07-01_积分体系_v1.0 | 2026-07-15 | 积分充值/消耗/流水 |

## 模块索引

| 模块 | 现状文档 | 状态 | 最后更新 |
|------|---------|------|---------|
| farvis-video | → project/modules/farvis-video.md | ✅ 已上线 | 2026-06-19 |
| farvis-credits | → project/modules/farvis-credits.md | 🔄 开发中 | 2026-07-01 |

## 迭代历史摘要

| 迭代 | 归档位置 | 核心变更 |
|------|---------|---------|
| 2026-06-19_基础框架_v0.1 | → archive/2026-06-19_基础框架_v0.1/ | 项目骨架 + 视频/形象/声音模块 |
```

## 与 .harness/ai-context/ 的关系

```
Docs/AI-CONTEXT.md              → 摘要+索引（Agent 日常主要读这个）
Docs/project/                   → 详情（需要深入了解时跳转）
.harness/ai-context/*.yaml      → 结构化查询（编码决策时精确查询）
```

**读取时机**：
- Agent 启动时：读 AI-CONTEXT.md 快速了解全局
- 需要业务细节时：跳转到 Docs/project/modules/*.md
- 编码决策时：查询 .harness/ai-context/*.yaml

## 迭代生命周期示例

### 1. 创建迭代

```bash
# 用户从 main 创建新分支
git checkout -b 2026-07-01_积分体系_v1.0

# post-checkout hook 自动：
# 1. 写入 .harness/inbox/evt_xxx_branch-created.json
# 2. 提示 Agent 创建迭代目录

# Agent 响应事件：
# 1. 创建 Docs/iterations/2026-07-01_积分体系_v1.0/
# 2. 从模板初始化 _meta.yaml、prd.md 等文件
```

### 2. 开发中

```
迭代进行中：
- prd.md：记录需求
- tech-design.md：记录技术方案
- tasks.md：追踪任务进度
- ddl-changes.md：记录表结构变更
- api-changes.md：记录接口变更
```

### 3. 归档迭代

```bash
# 用户将分支合并到 main
git checkout main
git merge 2026-07-01_积分体系_v1.0

# post-merge hook 自动：
# 1. 写入 .harness/inbox/evt_xxx_branch-merged.json
# 2. 提示 Agent 执行归档

# Agent 响应事件（执行 harness-archive-iteration Skill）：
# 1. 合并 ddl-changes.md → Docs/project/data-model.md
# 2. 合并 api-changes.md → Docs/project/api-contracts.md
# 3. 移动迭代目录到 Docs/archive/
# 4. 调用 harness-sync-context Skill 更新 AI-CONTEXT.md
```

## 注意事项

- 本目录是**结构示例**，不包含实际内容
- 实际使用时，参考 `core-design/templates/` 下的模板
- 详见 `core-design/03-systems-integration.md` 了解新旧体系整合
