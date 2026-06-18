# Phase 3：Spec & 开发

> **定位：** 基于 PRD + 架构设计，对单个任务做 Spec 分析并完成 TDD 开发。
> **Agent 帽子：** 需求分析师 → 技术架构师 → 开发者 → 测试工程师
> **前置：** Phase 1 的 PRD + Phase 2 的架构设计/任务清单（如有）
> **输入：** 单个任务（Phase 2 任务 / 直接从 PRD 拆出的切片组）
> **产出：** Spec → 技术设计 → 源代码 → 测试 → 自验

---

## 启动检查（新增）

Phase 3 启动时，Agent 先做三件事：

### 0. Harness 预检（Java 项目）

如果项目根目录存在 `.harness/`，**优先加载**：

```
1. 读 .harness/ai-context/project-map.yaml    → 模块边界与依赖
2. 读 .harness/ai-context/business-rules.yaml → 幂等/缓存/一致性约束
3. 读 .harness/ai-context/coding-rules.yaml   → 编码规则
4. 如有 .harness/ai-context/coding-rules-*.yaml → 框架特化规则
5. 读 .harness/ai-context/error-catalog.yaml  → 错误码参考
```

如果文件为空模板（刚初始化未填写）→ 按 `.harness/skills/harness-java.md` 的退化路径处理：有 PRD→提示提取，无 PRD→提示填写，拒绝→跳过并标注 `[上下文缺失]`。

### 1. 检查 Phase 2 是否存在

| 情况 | 处理 |
|------|------|
| Phase 2 已完成 → 有任务清单 | 读 `docs/architecture/task-list-*.md`，按任务逐个执行 |
| Phase 2 被跳过 → 无任务清单 | Agent 从 PRD 直接拆切片组，**第一个切片前执行「全局设计 fallback」** |

### 2. 全局设计 fallback（仅当 Phase 2 被跳过时）

如果跳过 Phase 2，Agent 在第一个切片的技术设计之前，额外产出全局设计：

```
全局设计（一次性，后续任务复用）
  ├── 技术栈确认（语言/框架/数据库/缓存/MQ）
  ├── 模块边界（如果是多模块项目）
  ├── 命名约定（包名/类名/API 路径前缀/响应格式）
  └── 共享 Entity（多任务共用的基础实体）

写入：docs/technical/global-design.md
```

> **设计原则：** 全局设计只写「约束其他切片的设计决策」，不写单切片的实现细节。后续每个切片的技术设计都引用这份全局设计。

### 3. 环境自检

每个切片开始开发前，Agent 检查本地环境是否就绪：

```bash
# Java Harness 项目
.harness/devops/env-check.sh
```

如果环境不可用：
- Docker 未运行 → 提示用户启动 Docker
- 容器未启动 → 执行 `docker compose -f .harness/devops/docker-compose.yml up -d`
- MySQL/Redis 连接失败 → 等待 10s 重试，仍失败则提示用户排查

环境检查通过后输出：「✅ 环境就绪：MySQL/Redis/WireMock 全部可用」

如果项目未接入 Harness → 跳过环境自检。

---

## 流程总览

```
任务（Phase 2 任务 或 PRD 切片组）+ 全局设计（如有）
    ↓
阶段 ① Spec 生成（决策树 → 概要 → 确认 → 详细 Spec）
    ↓
阶段 ② 技术设计（API + Entity + DB，一份文档）
    ↓
阶段 ③ 前端设计（条件性：仅复杂逻辑时触发）
    ↓
阶段 ④~⑧ 垂直切片 TDD 循环
    ├── ④ 任务拆解（本切片）
    ├── ⑤ 写测试代码
    ├── ⑥ 跑测试（RED）
    ├── ⑦ 编码实现
    └── ⑧ 编译 + 测试通过（GREEN）
    ↓
阶段 ⑨ 自验
    ↓
PR review + 验收 → 下一任务
```

---

## Spec 决策树（阶段 ①）

核心原则从 `rules/decision-boundary.md`，三个阶段通用：

| 分支 | 触发条件 | 处理方式 |
|:----|------|------|
| **分支 A：Agent 自定** | project.md 有约定 / 行业标准 / 项目已有同类参考 / 改动 ≤ 50 行 | 直接定，概要中一句告知 |
| **分支 B：必须问用户** | 影响数据/范围/架构/外部系统 / PRD 完全没提 | 给 2~3 选项 + 建议 |
| **分支 C：暂定 + 标记**（新增） | 既不满足 A 也不满足 B / 可临时用默认做法顶替 | 用最通用做法，标注 `[待确认]`，概要中统一确认 |

### 决策点上限

- 分支 B 问题每轮 ≤ **3 个**
- 超过 3 个 → 优先问数据策略/架构选型，其余走分支 C
- 分支 C 不限数量，但每个都要写「暂定选了什么 + 为什么」

---

## TDD 铁律

1. 必须先跑出 RED 再写实现
2. RED 失败原因是「代码未实现」而非测试写错 → 否则先修测试
3. GREEN 不通过时改代码，不改测试
4. 每切片完成后通报用户进度

### TDD 五步（Java Harness 增强）

如果项目存在 `.harness/templates/`，扩展为五步。
详见：`shared/tdd-five-steps.md`（入口/出口条件、命令、跳过规则、决策矩阵）

```
① 编译 + 静态规则检查（增量编译 ≤ 3s）
   → 失败 → 修复编译/规则问题
② 单元测试（Mock 外部依赖）
   → RED → 进入实现
③ 模块切片测试（只装配当前模块 Bean ≤ 30s）
   → 涉及 Spring Bean 装配时必写
④ 集成测试（Testcontainers 按需触发）
   → 涉及 DB/缓存/外部 API 时必写
⑤ REFACTOR
```

生成代码时参考 `.harness/templates/` 下的模板。

### TDD 命令

```bash
# 切片级快速反馈（Java Harness 标配）
./gradlew fastTest          # 增量编译 + 切片测试，≤ 3s
./gradlew test              # 全量单元+切片测试，≤ 30s
./gradlew integrationTest   # 集成测试（Testcontainers），按需触发
```

> `fastTest` task 定义见 `.harness/devops/build.gradle.kts`。Agent 每次编码后先跑 `fastTest`，通过再继续。

---

## 设计冲突处理

1. 先看技术设计文档——它是约定
2. 设计有问题 → 更新设计文档再编码
3. 实现细节选择 → Agent 自主决定
4. 影响设计的冲突 → 升级给用户 `[偏离 · 类型]`

---

## 阶段 ⑨ 自验（原「集成验证」）

> ⚠️ **这是自验，不是集成测试。**

| 维度 | Phase 3 自验 | Phase 4 集成测试 |
|------|------|------|
| **范围** | 本任务 + 已有代码 | 全量（所有任务 + 外部依赖） |
| **目的** | 验证本任务没搞坏别人 | 验证整个系统端到端可工作 |
| **环境** | 开发环境，可能只启动相关模块 | 类生产环境，启动所有服务 |
| **产出** | 编译通过 + 已有测试不回归 | 集成测试报告 |

### 自验清单

- [ ] 本任务代码编译通过
- [ ] 本任务测试全部 GREEN
- [ ] 已有测试无回归（全量跑一遍）
- [ ] 如涉及 API：用 curl/脚本调通本任务新增接口
- [ ] 如涉及页面：页面可正常渲染

---

## 状态跟踪

每完成阶段/切片 → 更新状态文件。
模板：`../../shared/state.json`

## Phase 3 退出条件

当前任务的所有切片 TDD 完成 + 自验通过 + 用户 PR review 通过 → 进入下一个任务或 Phase 4。
