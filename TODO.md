# Java Harness 建设 TODO

> 最后更新：2026-06-17
> Harness = core（框架无关原理） + stacks（技术栈实现） + examples（验证案例）
> 项目初始化后全部归入 `.harness/`，软链接接入各工具

---

## A. 流程层 — AI 怎么干活

| # | 项目 | 状态 | 说明 |
|:--|------|:--:|------|
| A1 | harness-dev-flow Skill（4 Phase 总入口） | ✅ | v2.9.0，Phase 1→4 路由、决策树 |
| A2 | Phase 1 flow.md（产品·原型） | ✅ | STEP 0~6.5，含产品形态识别 + error-catalog 同步提取 |
| A3 | Phase 2 flow.md（架构设计） | ✅ | 303 行：启动 Harness 预检 + STEP 2.4 project-map 同步 + STEP 3 ADR 写入 .harness/ |
| A4 | Phase 3 flow.md（Spec·开发） | ✅ | TDD 切片流程 + 决策边界 + 环境自检 |
| A5 | Phase 4 flow.md（集成测试） | ✅ | 357 行：启动 Harness 预检 + 契约测试(3.3) + 观测验证(3.4) + 详细报告格式 |
| A6 | decision-boundary.md（三支决策树） | ✅ | 分支 A/B/C，Phase 1&3 通用 |
| A7 | Phase 1/2/3/4 模板文件 | ✅ | prd.md / architecture.md / spec.md 等 |
| A8 | **流程层引用 .harness/** | ✅ | Phase 1→4 flow.md 全部接入 |
| A9 | Phase 3 TDD 5 步详细流程 | ✅ | `shared/tdd-five-steps.md`：每步入口/出口条件、命令、跳过规则、决策矩阵 |
| A10 | Phase 3 环境自检 | ✅ | 启动检查 3：切片前调用 env-check.sh，失败则自动启动 docker compose |

**待完成**：0 项

---

## B. 上下文层 — AI 知道什么

| # | 项目 | 状态 | 说明 |
|:--|------|:--:|------|
| B1 | core/ai-context/ 空模板（4 个 YAML） | ✅ | project-map / business-rules / error-catalog / coding-rules |
| B2 | 上下文 schema 不绑 Spring | ✅ | 框架特化规则移入 stacks/ |
| B3 | ~~project-context.md（入口索引）~~ | ❌ 已删除 | AGENTS.md 已直接指向 `.harness/ai-context/`，此文件冗余 |
| B4 | examples/farvis-ai/ 填写示例 | ✅ | project-map（6 模块）+ business-rules（6 规则）+ error-catalog（15 错误） |
| B5 | Phase 1 → business-rules.yaml 自动生成 | ✅ | Phase 1 STEP 6.4：结构化提取指南 + 6.4.5 error-catalog 同步 |
| B6 | Phase 2 → project-map.yaml 产出 | ✅ | Phase 2 STEP 2.4：模块划分完成后立即写入（含 project 元信息） |
| B7 | Phase 3 自动加载 `.harness/ai-context/` | ✅ | Phase 3 启动检查 0：优先加载 ai-context 4 文件，空则退化路径补偿 |
| B8 | Phase 4 扫描 error-catalog | ✅ | Phase 4 STEP 2.1：每个错误码至少一个触发路径测试 |
| B9 | 上下文版本管理 & 过期检测 | ✅ | `scripts/context-version-check.py`：mtime/hash 检测，> 7 天告警 |

**待完成**：0 项

---

## C. 反馈层 — AI 改完多久知道对不对

| # | 项目 | 状态 | 说明 |
|:--|------|:--:|------|
| C1 | build.gradle.kts | ✅ | 增量编译 + fastTest / test / integrationTest |
| C2 | infra/slice/ | ✅ | 只装配必要 Bean |
| C3 | 切片测试模板 | ✅ | 含并发测试 |
| C4 | 单元测试模板 | ✅ | Mock 外部依赖 |
| C5 | 集成测试模板 | ✅ | Testcontainers + WireMock |
| C6 | core/patterns/slice-testing.md | ✅ | 概念层 |
| C7 | Checkstyle / SpotBugs / ArchUnit 规则 | ✅ | checkstyle.xml + spotbugs-exclude.xml |
| C8 | Annotation Processor 白名单 | ✅ | ap-whitelist.yaml：Lombok + MapStruct 白名单 |
| C9 | 测试数据工厂 | ✅ | fixture-template.java：Builder + Consumer 模式 |
| C10 | 契约测试模板 | ✅ | contract-test-template.java：WireMock 五场景 |
| C11 | CI 流水线集成 | ✅ | ci-github-actions.yml：fast/full/integration 三层 CI |
| C12 | 反馈时间监控 | ✅ | benchmark-feedback.sh：实测耗时 vs SLA |

**待完成**：0 项

---

## D. 环境层 — AI 的代码在哪里跑

| # | 项目 | 状态 | 说明 |
|:--|------|:--:|------|
| D1 | core/devops/docker-compose.yml | ✅ | 通用占位 |
| D2 | stacks/docker-compose.yml | ✅ | MySQL + Redis + WireMock |
| D3 | core/patterns/outbox.md | ✅ | 概念层 |
| D4 | infra/outbox/ | ✅ | JPA 实现 |
| D5 | WireMock stub 示例 | ✅ | heygen-api.json：7 场景 |
| D6 | init-sql/ | ✅ | outbox_events DDL |
| D7 | application-test.yml | ✅ | Testcontainers 动态数据源 |
| D8 | application-local.yml | ✅ | Local profile + actuator 全端点 |
| D9 | 环境自检脚本 | ✅ | env-check.sh |
| D10 | 环境重置脚本 | ✅ | env-reset.sh |
| D11 | Maven (pom.xml) | ✅ | Spring Boot 3.3 + fast-test/integration-test profiles |
| D12 | Git hooks | ✅ | pre-commit/pre-push/commit-msg |
| D13 | AI 工具 hooks 目录 | ✅ | claude/codex/qoder |

**待完成**：0 项

---

## E. 观测层 — AI 怎么定位问题

| # | 项目 | 状态 | 说明 |
|:--|------|:--:|------|
| E1 | logback-spring.xml | ✅ | JSON + MDC traceId |
| E2 | BusinessMetricsTemplate.java | ✅ | Counter + Timer |
| E3 | core/principles/05-observability.md | ✅ | 概念层 |
| E4 | OpenTelemetry / Tracing | ✅ | application-otel.yml：OTLP + Jaeger |
| E5 | Micrometer + Prometheus | ✅ | application-actuator.yml + prometheus.yml |
| E6 | 业务指标清单模板 | ✅ | metrics-checklist.yaml |
| E7 | Grafana Dashboard JSON | ✅ | 5 行 15 面板 |
| E8 | Prometheus 告警规则 | ✅ | alert-rules.yml：10 条 |
| E9 | Error Catalog → Metrics 联动 | ✅ | ErrorCodeMetrics.java |
| E10 | JFR / Profiling 指引 | ✅ | jfr-profiling-guide.md |

**待完成**：0 项

---

## F. 代码模式层 — AI 按什么模板写代码

| # | 项目 | 状态 | 说明 |
|:--|------|:--:|------|
| F1 | controller-template.java | ✅ | |
| F2 | service-template.java | ✅ | 幂等 + Outbox |
| F3 | repository-template.java | ✅ | |
| F4 | entity-template.java | ✅ | 状态方法 + 工厂方法 |
| F5 | dto-template.java | ✅ | |
| F6 | 测试模板（三层） | ✅ | unit/slice/integration |
| F7 | core/patterns/（4 个概念模式） | ✅ | outbox/idempotency/circuit-breaker/slice-testing |
| F8 | GlobalExceptionHandler 模板 | ✅ | 统一异常 + 错误码映射 |
| F9 | Result\\<T\\> 返回体模板 | ✅ | {code, data, message} |
| F10 | IdempotencyService 模板 | ✅ | Redis SETNX + TTL |
| F11 | ExternalApiClient 模板 | ✅ | 超时+重试+三态熔断+降级 |
| F12 | CircuitBreaker 配置模板 | ✅ | application-resilience4j.yml |
| F13 | Maven (pom.xml) | ✅ | 同 D11 |
| F14 | 模板参数化 | ✅ | parameterize.py |

**待完成**：0 项

---

## G. Harness Skill — Agent 如何用这个仓库

| # | 项目 | 状态 | 说明 |
|:--|------|:--:|------|
| G1 | SKILL.md — harness-java-init | ✅ | 检测→选栈→复制→软链接→参数化→AGENTS.md |
| G2 | 技术栈检测 | ✅ | 读 pom.xml/build.gradle |
| G3 | 新/老项目区分 | ✅ | AGENTS.md 不存在→新建，存在→追加 |
| G4 | AGENTS.md / CLAUDE.md 生成 | ✅ | 渐进披露 |
| G5 | 软链接机制 | ✅ | git hooks + skill symlinks |
| G6 | harness-java.md（运行时 Skill） | ✅ | 先读上下文→按模板→TDD→查 error-catalog |
| G7 | 与 harness-dev-flow 衔接 | ✅ | 报告末尾「开始 Phase 1」 |
| G8 | Skill 测试（Farvis-AI 端到端） | ✅ | Sub-agent 验证通过 |

**待完成**：0 项

---

## H. 跨层整合

| # | 项目 | 状态 | 说明 |
|:--|------|:--:|------|
| H1 | core/ 与 stacks/ 引用一致性 | ✅ | stack README 标注 core 原理映射 |
| H2 | README.md（仓库定义） | ✅ | 六层 + 项目结构 + 软链接 |
| H3 | 端到端验证（Farvis-AI Phase 1→4） | ✅ | Sub-agent 验证通过 |
| H4 | 新 stack 开发指南 | ✅ | docs/new-stack-guide.md |
| H5 | CHANGELOG | ✅ | CHANGELOG.md |

**待完成**：0 项

---

## 统计

| 层 | ✅ | ⬜ | % |
|----|:--:|:--:|:--:|
| A. 流程层 | 10 | 0 | 100% |
| B. 上下文层 | 9 | 0 | 100% |
| C. 反馈层 | 12 | 0 | 100% |
| D. 环境层 | 13 | 0 | 100% |
| E. 观测层 | 10 | 0 | 100% |
| F. 代码模式层 | 14 | 0 | 100% |
| G. Harness Skill | 8 | 0 | 100% |
| H. 跨层整合 | 5 | 0 | 100% |
| **总计** | **81** | **0** | **100%** |

🎉 Java Harness 全部 81 项完成。
