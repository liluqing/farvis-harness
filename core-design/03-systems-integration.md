# 核心设计 03：新旧体系整合规范

> **文档定位**：定义 flow/（开发流程）与 Docs/（文档管理）两套体系的整合关系，消除冲突。
>
> **创建日期**：2026-06-19

---

## 1. 整体架构：四层信息模型

原来的三层模型扩展为四层，明确各层的定位和关系：

```
┌──────────────────────────────────────────────────────────┐
│  第一层：AI-CONTEXT.md（Agent 工作记忆）                    │
│  定位：Docs 的摘要 + 索引                                  │
│  用途：Agent 启动时快速了解全局，< 2000 字                  │
│  维护：同步 Skill 自动维护                                  │
├──────────────────────────────────────────────────────────┤
│  第二层：Docs/project/（项目当前状态）                      │
│  定位：项目现状的唯一真相源（人类可读）                      │
│  用途：Agent 需要详情时通过索引跳转                         │
│  维护：仅在迭代归档时更新                                   │
├──────────────────────────────────────────────────────────┤
│  第三层：.harness/ai-context/*.yaml（结构化上下文）          │
│  定位：Agent 做精确决策时的结构化查询源                      │
│  用途：模块边界、业务规则、错误码、编码规范                   │
│  维护：各 Phase 按职责追加更新                              │
├──────────────────────────────────────────────────────────┤
│  第四层：Docs/iterations/ + archive/（变更历史）             │
│  定位：迭代过程记录                                        │
│  用途：回溯决策过程，归档后汇入第二层                       │
│  维护：迭代进行中由人和 Agent 协同维护                      │
└──────────────────────────────────────────────────────────┘
```

### 各层读取时机

| 时机 | 读取哪些层 |
|------|-----------|
| Agent 启动 / 会话初始化 | 第一层（AI-CONTEXT.md）+ 第三层（ai-context/*.yaml） |
| 需要某个模块的业务细节 | 第二层（project/modules/*.md） |
| 回溯历史决策 | 第四层（archive/{迭代名}/） |
| 开发中查模块边界/编码规则 | 第三层（ai-context/*.yaml） |
| 归档时更新项目状态 | 第四层 → 第二层 → 第一层（依次更新） |

### 第二层 vs 第三层的分工

| 维度 | 第二层（Docs/project/） | 第三层（ai-context/*.yaml） |
|------|------------------------|---------------------------|
| 格式 | Markdown（人类友好） | YAML（机器友好） |
| 内容 | 业务描述、流程、架构全景 | 结构化数据：模块列表、规则、错误码 |
| 粒度 | 段落级描述 | 字段级精确 |
| 谁读 | Agent 了解业务背景 | Agent 做编码决策 |
| 谁写 | 归档 Skill 合并 | 各 Phase 按职责追加 |

**核心原则**：第二层告诉 Agent "业务是怎么回事"，第三层告诉 Agent "代码该怎么写"。

---

## 2. Phase 产出 → 迭代文档映射

开发流程（flow/skill.md）的每个 Phase 产出，写入迭代目录的对应文件：

| Phase | 产出 | 写入迭代目录的文件 | 说明 |
|-------|------|-------------------|------|
| Phase 1（需求确认） | PRD | `prd.md` | 直接写入 |
| Phase 1 | Harness 上下文 | `.harness/ai-context/business-rules.yaml` + `error-catalog.yaml` | 追加到第三层 |
| Phase 2（架构设计） | 架构文档 | `tech-design.md`（架构设计部分） | 写入迭代技术设计 |
| Phase 2 | 任务清单 | `tasks.md` | 写入迭代任务清单 |
| Phase 2 | Harness 上下文 | `.harness/ai-context/project-map.yaml` | 追加到第三层 |
| Phase 3（逐切片开发） | 技术设计（每切片） | `tech-design.md`（追加切片设计） | 追加到同一个 tech-design.md |
| Phase 3 | 代码 + 测试 | `src/`（项目源码目录） | 不进迭代目录 |
| Phase 3 | 状态追踪 | `.harness/flow/shared/state.json` | 开发进度追踪 |
| Phase 3 | DDL 变更 | `ddl-changes.md` | 如有表结构变更 |
| Phase 3 | API 变更 | `api-changes.md` | 如有接口变更 |
| Phase 4（集成验证） | 集成测试报告 | `review-notes.md`（测试报告部分） | 追加到评审记录 |
| 归档 | 迭代目录 → archive/ | 触发归档 Skill | 合并到 project/ + 同步 AI Context |

### tech-design.md 的结构约定

一个迭代的 `tech-design.md` 可能包含 Phase 2 和 Phase 3 的多次写入：

```markdown
# 技术设计文档

## 架构设计（Phase 2 产出）
...

## 切片 1：{切片名}（Phase 3 产出）
...

## 切片 2：{切片名}（Phase 3 产出）
...
```

---

## 3. state.json 与 _meta.yaml 的分工

两者并存，各司其职，不存在替代关系。

| 维度 | state.json | _meta.yaml |
|------|-----------|-----------|
| 位置 | `.harness/flow/shared/state.json` | `Docs/iterations/{迭代名}/_meta.yaml` |
| 职责 | **开发进度追踪** | **迭代元信息** |
| 粒度 | 切片级（每个切片的 TDD 步骤） | 迭代级（整体状态和概要） |
| 更新频率 | 每个切片/每步 TDD 都更新 | 迭代创建、完成、归档时更新 |
| 谁用 | Agent 断点续接时读取 | 归档 Skill、AI Context 同步时读取 |
| 生命周期 | 迭代进行中频繁更新，归档后可冻结 | 归档后变为只读 |

### state.json 包含而 _meta.yaml 不包含的

- 每个切片的详细状态（pending/in_progress/completed/failed）
- 当前切片的 TDD 步骤（①~⑤）
- 自修次数（self_repair_count）
- 各 Phase 的细粒度状态

### _meta.yaml 包含而 state.json 不包含的

- 迭代名称、版本号
- 影响模块列表（modules_affected）
- 一句话摘要（summary）
- 归档信息（archive_info）
- 外部引用链接（external_refs）

### 数据流向

```
state.json（开发中持续更新）
    ↓ 归档时
_meta.yaml 的 status 改为 archived
    ↓
state.json 中的迭代记录可标记为 completed（保留但不再更新）
```

---

## 4. Skill 加载关系

### 4.1 全量 Skill 清单

| Skill | 位置 | 触发时机 |
|-------|------|---------|
| `harness-dev-flow` | `flow/skill.md` | 用户提出开发需求时 |
| `harness-java-init` | `SKILL.md` | 用户要求初始化 Harness 时 |
| `harness-java` | `.harness/skills/harness-java.md` | Agent 执行开发任务时 |
| `harness-sync-context` | `.harness/skills/harness-sync-context.md` | 归档后 / 手动触发 / 定时 |
| `harness-archive-iteration` | `.harness/skills/harness-archive-iteration.md` | 迭代完成时 |

### 4.2 调用链

```
用户说"开始开发 XX"
    ↓
harness-dev-flow（flow/skill.md）
    ├── Phase 1 → 产出写入 Docs/iterations/{迭代名}/prd.md
    ├── Phase 2 → 产出写入 tech-design.md + tasks.md
    ├── Phase 3 → 产出追加 tech-design.md + ddl-changes.md + api-changes.md
    │   └── 调用 harness-java（运行时 Skill）
    └── Phase 4 → 产出追加 review-notes.md

迭代完成，分支合并到 main
    ↓
post-merge hook → 写入 inbox 事件
    ↓
Agent 读取事件 → 调用 harness-archive-iteration
    ├── 合并文档到 Docs/project/
    ├── 移动迭代目录到 Docs/archive/
    └── 调用 harness-sync-context
        ├── 更新 Docs/AI-CONTEXT.md
        └── 更新 Docs/.ai-context-sync.json
```

### 4.3 会话初始化协议（更新版）

flow/skill.md 中的会话初始化协议，更新为读取新体系的文件：

```
步骤 1：检查 Harness 项目（不变）

步骤 2：恢复上下文
  1. Docs/AI-CONTEXT.md                    → 项目全局概览
  2. .harness/ai-context/context.yaml      → 结构化上下文（模块/规则/错误码）
  3. .harness/flow/shared/state.json       → 当前开发进度
  4. Docs/iterations/                      → 活跃迭代列表
  5. .harness/inbox/                       → 待处理事件

步骤 3：输出项目状态摘要
  - 从 AI-CONTEXT.md 提取项目概览
  - 从 state.json 提取开发进度
  - 从 inbox/ 提取待处理事件
```

---

## 5. .harness/inbox/ 与 flow/ 的关系

inbox 是 flow/ 的**外部事件补充**，两者不冲突：

| 维度 | flow/（开发流程） | inbox/（事件信箱） |
|------|------------------|-------------------|
| 触发方 | 用户主动发起 | 外部系统（Git Hook / CI） |
| 内容 | "我要开发 XX" | "分支创建了/合并了/CI 失败了" |
| 处理 | 进入 Phase 路由 | 执行 required_actions |
| 时机 | 用户说话时 | Agent 启动时扫描 |

**Agent 启动时的处理顺序**：
1. 先处理 inbox 中的 high priority 事件
2. 再执行会话初始化协议
3. 最后响应用户输入

---

## 6. 文件路径对照表（旧 → 新）

供 Agent 和人类参考，明确文档应该放在哪里：

| 旧路径（flow/skill.md 中原定义） | 新路径（Docs/ 体系） | 说明 |
|------|------|------|
| `docs/product/prd-<功能名>.md` | `Docs/iterations/{迭代名}/prd.md` | 迭代内集中存放 |
| `docs/architecture/architecture-<功能名>.md` | `Docs/iterations/{迭代名}/tech-design.md` | 合并到技术设计 |
| `docs/architecture/task-list-<功能名>.md` | `Docs/iterations/{迭代名}/tasks.md` | 迭代内集中存放 |
| `docs/design/design-<功能名>-<切片名>.md` | `Docs/iterations/{迭代名}/tech-design.md` | 追加到同一个文件 |
| `docs/integration-test/report-<功能名>.md` | `Docs/iterations/{迭代名}/review-notes.md` | 追加到评审记录 |
| `docs/product/` | `Docs/project/modules/` | 项目现状按模块组织 |
| `docs/architecture/` | `Docs/project/architecture.md` | 项目现状集中 |

**注意**：flow/skill.md 和 SKILL.md 中的旧路径引用需要同步更新。
