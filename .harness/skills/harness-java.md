---
name: harness-java
description: Java 项目 Harness 运行时 Skill——Agent 在日常开发中自动加载，读取 .harness/ai-context/ 作为决策上下文，使用模板生成代码，执行切片测试和集成测试。
version: 0.3.0
tags: [harness, java, runtime, spring-boot, development]
triggers:
  - "开始.*Phase"
  - "开发.*功能"
  - "写.*代码"
  - "修复.*bug"
  - "实现.*接口"
  - "添加.*测试"
  - "重构.*"
---

# Harness Java 运行时 Skill

当用户请求开发任务（写代码、修 bug、加功能）时，此 Skill 自动加载。

## 核心流程

### 1. 先读上下文

在写任何代码之前，读取：

```
.harness/ai-context/project-map.yaml    → 模块边界
.harness/ai-context/business-rules.yaml → 业务约束
.harness/ai-context/coding-rules.yaml   → 通用编码规则
.harness/ai-context/error-catalog.yaml  → 错误码
```

如果有 stack 特化规则，也一起读（如 `.harness/ai-context/coding-rules-spring-boot.yaml`）。

### 1a. 上下文为空时的退化路径

如果 `.harness/ai-context/` 下的 YAML 文件全部为空模板（刚初始化未填写）：

1. **有 PRD** → 对用户说「检测到 PRD，是否从中提取上下文自动填写 ai-context/？」
2. **无 PRD** → 对用户说「Harness 上下文尚未填写，建议先填写 project-map.yaml（模块边界），我可以先按通用规则开发」
3. **用户拒绝填写** → 跳过上下文加载，按通用编码规则 + 模板生成代码。在每个 Spec 中标注 `[上下文缺失]`，提醒后续补充

### 2. 理解原理和模式

遇到以下场景时，参考原理和模式文档：

| 场景 | 参考文件 |
|------|---------|
| 不确定验证策略 | `.harness/principles/01-fast-feedback.md` |
| 不确定模块边界怎么定义 | `.harness/principles/02-context-contract.md` |
| 不确定测试怎么写 | `.harness/principles/03-auto-verification.md` |
| 涉及 DB+消息 | `.harness/patterns/outbox.md` |
| 涉及重复提交 | `.harness/patterns/idempotency.md` |
| 涉及外部 API | `.harness/patterns/circuit-breaker.md` |
| 不确定测试粒度 | `.harness/patterns/slice-testing.md` |

### 3. 按模板生成

生成代码时参考：

```
.harness/templates/controller-template.java
.harness/templates/service-template.java
.harness/templates/repository-template.java
.harness/templates/entity-template.java
.harness/templates/dto-template.java
.harness/templates/test/unit-test-template.java
.harness/templates/test/slice-test-template.java
.harness/templates/test/integration-test-template.java
```

关键约束（来自 coding-rules.yaml）：
- Controller 不写业务逻辑，只做协议转换 + 参数校验
- 跨聚合操作通过 ApplicationService 编排
- 消息发送用 Outbox 模式（参考 `.harness/infra/outbox/`）
- 需要幂等的操作参考 `.harness/patterns/idempotency.md`

### 3. 开发流程

进入 `harness-dev-flow` Skill 的四阶段流程：
- Phase 1（产品·原型）→ 产出 PRD
- Phase 2（架构设计）→ 产出 project-map.yaml + ADR
- Phase 3（Spec·开发）→ TDD 开发
- Phase 4（集成测试）→ 全量验证

Phase 3 的五步 TDD（详见 `shared/tdd-five-steps.md`）：
```
① 编译 + 静态规则（增量编译 ≤ 3s）
② 单元测试（Mock 外部依赖）
③ 模块切片测试（只装配当前模块 ≤ 30s）
④ 集成测试（Testcontainers 按需）
⑤ REFACTOR
```

### 4. 运行测试

```bash
# 快速反馈（切片测试，需先在项目 build.gradle.kts 中配置 fastTest task）
./gradlew fastTest

# 全量测试
./gradlew test

# 集成测试（慢，需配置 integrationTest task）
./gradlew integrationTest

# 注意：build.gradle.kts 的 fastTest/integrationTest task 定义参考
# .harness/devops/build.gradle.kts（模板仓库中的源文件）
# Agent 提示用户合并到项目构建文件后即可使用。
```

### 5. 本地环境

```bash
# 启动
docker compose -f .harness/devops/docker-compose.yml up -d

# 检查
docker compose -f .harness/devops/docker-compose.yml ps

# 外部 API 模拟 stubs 放在 .harness/devops/wiremock/stubs/
```

---

## 模式速查

| 模式 | 位置 | 何时使用 |
|------|------|---------|
| Outbox | `.harness/infra/outbox/` + `.harness/patterns/outbox.md` | DB 写 + 消息发送 |
| 幂等 | `.harness/infra/idempotency/` + `.harness/patterns/idempotency.md` | 支付/创建/扣减 |
| 外部 API | `.harness/infra/client/ExternalApiClient.java` + `.harness/patterns/circuit-breaker.md` | 所有外部 HTTP 调用 |
| 全局异常 | `.harness/infra/exception/GlobalExceptionHandler.java` | 统一错误响应 |
| 统一返回 | `.harness/infra/Result.java` | 所有 API 响应 |
| 熔断 | `.harness/patterns/circuit-breaker.md` | 外部 API 失败保护 |
| 切片测试 | `.harness/patterns/slice-testing.md` + `.harness/infra/slice/` | 每个模块 |
| 观测 | `.harness/infra/observe/BusinessMetricsTemplate.java` + `.harness/devops/metrics-checklist.yaml` | 指标定义与埋点 |

---

## 原则

1. **先读 .harness/ai-context/，再写代码** — 不了解项目规则不写代码
2. **模板不是建议，是规范** — 生成的代码必须符合模板约定
3. **错误先查 error-catalog** — 出问题时先看 `.harness/ai-context/error-catalog.yaml`
4. **Harness 是基础设施，不改业务代码** — `.harness/` 下的文件是支撑，不是业务
