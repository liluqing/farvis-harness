# Phase 4：集成测试

> **定位：** 全部任务完成后，启动完整环境，执行端到端集成测试，验证系统整体可工作。
> **执行角色：** 测试工程师
> **前置：** Phase 3 所有任务完成 + 自验通过
> **输入：** 所有已完成任务的代码 + PRD + 架构文档
> **产出：** 集成测试报告

---

## 启动检查

Phase 4 启动时，Agent 先做三件事：

### 0. 读取状态文件

读取 `.harness/flow/shared/state.json`，确认：
- `phases.phase3.status` 为 "completed"
- `slices` 中所有切片状态为 "completed"
- `phases.phase3.completed_tasks` 数量与 `phases.phase3.total_tasks` 一致

如果状态文件不存在或状态不符合要求 → 提示用户「Phase 3 尚未完成，无法进入集成测试」。

### 1. Harness 预检（Java 项目）

如果项目根目录存在 `.harness/`，**优先加载**：

```
1. 读 .harness/ai-context/project-map.yaml    → 模块依赖图（跨模块测试依据）
2. 读 .harness/ai-context/business-rules.yaml → 幂等/并发/降级规则
3. 读 .harness/ai-context/error-catalog.yaml  → 错误码清单（每个码至少一个用例）
4. 读 .harness/ai-context/coding-rules.yaml   → 编码规则（ArchUnit 校验）
```

加载后输出：「已加载 `.harness/ai-context/`：X 个模块、Y 条业务规则、Z 个错误码」

### 2. 确认前置条件

- [ ] Phase 3 所有任务代码已合并
- [ ] `./gradlew fastTest` 全部 GREEN（如有 Harness）
- [ ] 环境自检：`bash .harness/devops/env-check.sh`（失败则自动 `docker compose up -d` 重试）

> 如果 `env-check.sh` 不存在或无 `.harness/`，手动检查 `docker compose ps`。

### 3. 确认产出目标

Phase 4 的使命：**全量端到端验证，证明整个系统可工作。**

| 产出 | 写入位置 |
|------|---------|
| 集成测试报告 | `docs/integration-test/report-*.md` |
| 新增错误码 | `.harness/ai-context/error-catalog.yaml`（如有） |
| 契约测试文件 | `src/test/java/.../contract/`（如有外部 API） |

---

## Phase 4 vs Phase 3 自验

> ⚠️ 这个边界必须清楚，否则 Agent 会重复劳动。

| 维度 | Phase 3 阶段⑨（自验） | Phase 4（集成测试） |
|------|------|------|
| **时机** | 每个任务完成后 | 所有任务完成后 |
| **范围** | 本任务 + 已有代码不回归 | 全量：所有模块 + 外部依赖 |
| **环境** | 开发环境，可能只启动相关模块 | 完整环境：所有服务 + DB + 缓存 + MQ |
| **目的** | 本任务没搞坏别人 | 整个系统端到端可工作 |
| **测试类型** | 编译 + 单元测试 + 基础接口调通 | 端到端场景 + 跨服务数据一致性 + 异常恢复 |
| **产出** | 自验通过标记 | 集成测试报告 |
| **谁做** | Phase 3 Agent（开发完后顺手做） | Phase 4 Agent（独立的测试阶段） |

---

## 流程总览

```
所有 Phase 3 任务完成
    ↓
STEP 1: 环境准备
    ↓
STEP 2: 制定测试计划 → 用户确认
    ↓
STEP 3: 执行测试
    ↓
STEP 4: 问题修复（如有）→ 回归
    ↓
STEP 5: 输出测试报告
    ↓
STEP 6: ai-context 同步检查
    ↓
交付
```

---

## STEP 1：环境准备

### 1.1 检查依赖

| 检查项 | 来源 | 说明 |
|------|------|------|
| 数据库 | Phase 2 架构文档 / Phase 3 全局设计 | 确保 DB 已启动且表结构最新 |
| 缓存 | Phase 2 架构文档 | Redis 等是否可用 |
| 消息队列 | Phase 2 架构文档 | RabbitMQ/Kafka 等是否可用 |
| 外部服务 | PRD / 架构文档 | 第三方 API 是否需要 sandbox |

### 1.2 启动服务

```bash
# 按架构文档中的启动顺序，启动所有服务
# Agent 执行或指导用户执行
```

### 1.3 健康检查

- [ ] 所有服务健康检查通过
- [ ] 数据库连接正常
- [ ] 缓存/消息队列连接正常

---

## STEP 2：制定测试计划

### 2.1 测试场景来源

| 来源 | 提取方式 |
|------|------|
| PRD 核心场景 | 直接映射为端到端测试场景 |
| PRD 验收标准 | 逐条转化为测试用例 |
| 架构文档中的模块通信 | 跨模块数据一致性检查 |
| Phase 1 原型中的操作路径 | 映射为 UI 自动化场景（如有） |
| `.harness/ai-context/error-catalog.yaml` | 每个错误码至少一个触发路径测试 |
| `.harness/ai-context/business-rules.yaml` | 幂等/并发/降级规则 → 对应测试用例 |

### 2.2 测试场景格式

```
场景 X: <!-- 场景名 -->
  前置条件: <!-- 需要什么数据/状态 -->
  操作步骤:
    1. <!-- 步骤 -->
    2. <!-- 步骤 -->
  预期结果:
    - <!-- 结果 -->
  涉及模块: <!-- 跨哪些模块 -->
```

### 2.3 计划概要 → 用户确认

```markdown
【集成测试计划概要】

**环境：** <!-- 描述 -->

**测试场景：** N 个
- 场景 1: <!-- 核心 E2E 流程 -->
- 场景 2: <!-- 异常场景 -->
- ...

**预估耗时：** <!-- 约 X 分钟 -->

**需要你配合：** <!-- 如：启动 XX 服务、提供测试账号 -->
```

---

## STEP 3：执行测试

### 3.1 执行方式

Agent 通过以下方式执行测试：

| 方式 | 适用场景 | 工具 |
|------|------|------|
| HTTP 脚本 | REST API | curl / Python requests |
| 数据库查询 | 数据一致性验证 | SQL 直连 |
| 浏览器操作 | UI 流程 | Browser 工具（如已集成） |
| 单元/集成测试代码 | 已有测试用例 | 构建工具（mvn test / pytest） |

### 3.2 执行记录

每个场景记录：

```
场景 X: <!-- 场景名 -->
  执行时间: <!-- 时间戳 -->
  结果: ✅ PASS / ❌ FAIL / ⚠️ SKIP
  实际结果: <!-- 如果是 FAIL，记录实际发生了什么 -->
  证据: <!-- 截图/日志/响应 -->
```

### 3.3 契约测试（如有外部 API 依赖）

如果 PRD 或架构文档中明确有外部 API 调用（支付网关、AI 服务、短信等），在集成测试阶段**必须写入契约测试**。

**何时必写：**
- 调了外部 HTTP API（非本项目服务）
- 调了第三方 SDK 封装的远程服务
- 异步接收外部回调/Webhook

**契约测试格式：**

```java
// src/test/java/<base-pkg>/contract/<ExternalServiceName>ContractTest.java
// 放在 contract/ 包下，与业务测试分离

@Test
void contract_<provider>_<apiName>_<scenario>() {
    // Given: WireMock stub 定义的契约
    stubFor(post(urlEqualTo("/external/endpoint"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                {"code": 0, "data": {...}}
                """)));

    // When: 调本服务接口（触发外部调用）
    // Then: 验证本服务的响应 + 断言外部调用确实发生
    verify(postRequestedFor(urlEqualTo("/external/endpoint")));
}
```

**契约测试归类：**

|| 外部服务 | stub 位置 | 测试文件 |
||---------|----------|---------|
|| <!-- 如 HeyGen --> | `.harness/devops/wiremock/stubs/<service>.json` | `contract/HeyGenContractTest.java` |
|| <!-- 如 微信支付 --> | 同上 | `contract/WechatPayContractTest.java` |

> 如果项目未接入 Harness → stub 放在 `src/test/resources/wiremock/` 下。

### 3.4 观测验证

集成测试阶段验证观测基础设施是否就绪。**不做功能测试，只验证观测通路畅通。**

**检查清单：**

- [ ] `/actuator/health` 返回 UP
- [ ] `/actuator/prometheus` 返回非空 metrics（如已配置 Micrometer）
- [ ] 日志中可见 traceId（执行业务操作后 grep `grep "traceId"` 日志）
- [ ] Outbox 表无长期积压（`SELECT COUNT(*) FROM outbox_events WHERE status='PENDING' AND created_at < NOW() - INTERVAL 5 MINUTE`）

**如任一检查失败：**
- `/actuator/health` DOWN → 标注 `[环境问题]`，检查 docker compose
- `/actuator/prometheus` 空 → 标注 `[观测缺失]`，提醒：「Prometheus metrics 未暴露，生产环境无法监控」
- 无 traceId → 标注 `[观测缺失]`，检查 logback-spring.xml
- Outbox 积压 → 标注 `[数据问题]`，检查 OutboxPoller 是否运行

---

## STEP 4：问题修复

### 4.1 问题分类

| 类别 | 处理 | 谁处理 |
|------|------|------|
| Bug（代码缺陷） | 回到 Phase 3 对应切片修复 | Agent |
| 设计问题（架构不合理） | 升级给用户，附带建议 | 用户决策 |
| 环境问题（配置/网络） | Agent 尝试修复 | Agent |
| 数据问题（测试数据不完整） | Agent 补充测试数据 | Agent |

### 4.2 回归

修复后 → 重跑受影响场景 → 直到全部 PASS 或不可修复项已标记。

---

## STEP 5：输出测试报告

写入：`docs/integration-test/report-<功能名>.md`

### 报告格式

```markdown
# 集成测试报告：<功能名>

> **执行时间：** <YYYY-MM-DD HH:MM>
> **环境：** <docker compose / K8s>
> **测试范围：** <PRD 章节引用>

---

## 结果总览

| 指标 | 数值 |
|------|:--:|
| 总场景数 | N |
| ✅ PASS | N |
| ❌ FAIL | N |
| ⚠️ SKIP | N |
| 通过率 | XX% |

## 场景详情

### 场景 1: <场景名>

- **类型：** 核心 E2E / 异常场景 / 数据一致性
- **涉及模块：** <模块 1> → <模块 2>
- **结果：** ✅ PASS
- **耗时：** <X>ms
- **关键证据：** <!-- 如响应快照的关键字段 -->

### 场景 2: <场景名>

- **类型：** 异常场景
- **涉及模块：** <模块 1>
- **结果：** ❌ FAIL
- **预期：** <预期行为>
- **实际：** <实际行为>
- **分类：** Bug / 设计问题 / 环境问题 / 数据问题
- **建议：** <!-- 一句话 -->

## 契约测试（如有）

| 外部服务 | 场景 | 结果 |
|---------|------|:--:|
| <!-- HeyGen --> | 正常回调 | ✅ |
| <!-- 微信支付 --> | 超时重试 | ✅ |

## 观测验证

- [ ] Health check UP
- [ ] Prometheus metrics 暴露
- [ ] traceId 可见
- [ ] Outbox 无积压

## FAIL 项汇总

| 场景 | 分类 | 是否可修复 | 说明 |
|------|------|:--:|------|
| <!-- 场景 X --> | Bug | 是 | <!-- 一句话 --> |
| <!-- 场景 Y --> | 设计 | 否 | <!-- 原因 + 建议 --> |

## Harness 上下文更新

- [ ] 新增错误码已写入 `.harness/ai-context/error-catalog.yaml`（共 X 个）
- [ ] `coding-rules.yaml` 无需更新 / 已更新（<变更说明>）
- [ ] ArchUnit 规则全部通过

---

**结论：** 可交付 / 阻塞交付（<原因>）
```

---

## STEP 6：ai-context 同步检查

> ⚠️ 在提交最终代码前，确保 ai-context 文件与实际代码保持一致。

### 6.1 Diff ai-context 与实际代码变更

Agent 对比以下 ai-context 文件与本次开发过程中的实际代码变更：

| ai-context 文件 | 检查内容 |
|----------------|----------|
| `.harness/ai-context/project-map.yaml` 或 `core/ai-context/context.yaml` | 新增/删除的模块、API 端点变化、依赖关系变化 |
| `.harness/ai-context/business-rules.yaml` | 新增的幂等键、缓存策略、并发控制、降级规则 |
| `.harness/ai-context/error-catalog.yaml` | 新增的错误码（代码中 throw/return 的但目录中没有的） |
| `.harness/ai-context/coding-rules.yaml` | 新的编码约定、被违反后修复的规则 |

### 6.2 列出待同步项

Agent 输出以下清单供用户确认：

```
【ai-context 同步检查报告】

📋 新增错误码（X 个）：
- NEW_ERROR_CODE_1: cause / action / http_status
- NEW_ERROR_CODE_2: ...

🔌 新增 API 端点（X 个）：
- POST /api/v1/xxx → 归属模块 xxx-module
- GET /api/v1/yyy → 归属模块 yyy-module

📐 新增/变更业务规则（X 条）：
- [规则名]: [规则描述]

🗂️ 模块依赖变化（X 处）：
- [模块A] 新增依赖 [模块B]

⚠️ 编码规则补充（X 条）：
- [新规则描述]

无变化项：
- [ ] project-map ✅ 无需更新
- [ ] business-rules ✅ 无需更新
- [ ] error-catalog ✅ 无需更新
- [ ] coding-rules ✅ 无需更新
```

### 6.3 用户确认

Agent 提示用户：

```
以上是本次开发中发现的 ai-context 待同步项。
请确认是否需要更新 ai-context 文件？
- 确认更新 → Agent 自动写入对应文件
- 跳过 → 记录为 TODO，不阻塞交付
```

### 6.4 执行更新

用户确认后，Agent：

1. 将新增错误码追加到 `.harness/ai-context/error-catalog.yaml`
2. 将新增 API 端点更新到 `.harness/ai-context/project-map.yaml`
3. 将新增业务规则追加到 `.harness/ai-context/business-rules.yaml`
4. 将编码规则补充追加到 `.harness/ai-context/coding-rules.yaml`
5. 如果使用单文件版 `core/ai-context/context.yaml`，同步更新对应 section

> 更新完成后，Agent 输出：「✅ ai-context 已同步：X 个错误码、Y 个 API 端点、Z 条业务规则已更新」

---

## 阶段退出

### Done Checklist
- [ ] 所有服务正常启动
- [ ] 测试计划中所有场景已执行
- [ ] 关键 E2E 流程全部 PASS
- [ ] FAIL 项已分类（Bug / 设计 / 环境 / 数据）
- [ ] 不可修复项已标注原因 + 建议
- [ ] 测试报告已写入
- [ ] `.harness/ai-context/error-catalog.yaml` 中每个错误码至少一个用例 PASS（如项目已接入 Harness）
- [ ] 新增错误码已补充到 error-catalog.yaml（如有）

### 交付与迭代结束

Agent 输出：

```
✅ 迭代 1 完成。集成测试报告已就绪。

测试结果：
- X 个场景 PASS
- Y 个 FAIL（Z 个不可修复）

下一步建议：
- 如果可交付：说「开始迭代 2」进入下一个需求
- 如果有阻塞项：先处理不可修复项，再说「重新跑集成测试」
```

**迭代状态更新：**
- 更新 `.harness/flow/shared/state.json`：`iteration.status` 设为 "completed"
- 如果开始新迭代，`iteration.id` +1，`iteration.status` 重置为 "in_progress"

**是否需要用户触发：** 是。迭代结束后必须等用户决定下一个迭代的需求。

### Harness 回归（如项目已接入 Java Harness）

Phase 4 完成后，如果 `.harness/` 存在：

1. 本次发现的新错误码 → 写入 `.harness/ai-context/error-catalog.yaml`
2. 检查 `.harness/patterns/` 中的模式是否被遵循（Outbox/幂等/熔断）
3. 如有 ArchUnit 测试（`.harness/infra/arch/`）→ 确认 CI 通过
