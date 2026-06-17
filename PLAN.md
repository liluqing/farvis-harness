# Java Harness 改造计划

> 当前状态：100%（81/81 完成）。🎉 八个层全部完成。
> 剩余 0 项。

---

## 总体判断

基础设施骨架就位，但存在一个结构性断层：

> **harness-dev-flow Skill（流程层）和 Java Harness 基础设施（.harness/）互不认识。**

Agent 走 Phase 3 开发时，不会自动加载 `.harness/ai-context/`。流程和基础设施是两条平行线。

所以后续工作的核心主线是：**先接线，再补肉，最后抛光。**

---

## 第一波：接线（2 项，✅ 已完成 → 59%）

**目标：让 harness-dev-flow 流程能引用 `.harness/` 基础设施。**

| # | 内容 | 具体做什么 | 为什么先做 |
|:--|------|----------|------|
| A8 | 流程层引用 .harness/ | 修改 harness-dev-flow Skill 的 Phase 2/3/4 flow.md，加入"加载 `.harness/ai-context/`""执行 `./gradlew fastTest`""查看 `.harness/templates/`"等步骤 | 不做的话，剩下的所有东西（模板、规则、测试）Agent 都不会用 |
| H3 | Farvis-AI Phase 1→4 全流程 | 用 Farvis-AI PRD 走一个真实切片：Phase 1 产出 PRD → Phase 2 产出 project-map → Phase 3 TDD 开发 → Phase 4 集成测试 | 暴露"接线"后的集成问题。A8 改了流程文件但没实测 = 白改 |

**这波做完的判断标准**：Agent 对 Farvis-AI 说「开发视频生成接口」，Agent 自动加载 `.harness/ai-context/`，按模板生成代码，跑 `fastTest`。

---

## 第二波：补流程（3 项，✅ 已完成 → 63%）

**目标：填补流程层两个大窟窿。**

| # | 内容 | 具体做了什么 | 状态 |
|:--|------|----------|:--:|
| A3 | Phase 2 flow.md | 启动 Harness 预检 + STEP 2.4 project-map.yaml 同步 + STEP 3 ADR 写入 .harness/ai-context/adr/。238→303 行 | ✅ |
| A5 | Phase 4 flow.md | 启动 Harness 预检 + 3.3 契约测试 + 3.4 观测验证 + 详细报告格式。193→357 行 | ✅ |
| A9 | Phase 3 TDD 五步 | `shared/tdd-five-steps.md`：每步入口/出口条件、命令、跳过规则、决策矩阵 | ✅ |

**判断标准达成**：Agent 能独立走完 Phase 1→4 全流程，中间不需要人工干预流程选择。

---

## 第三波：补代码模式（3 项，✅ 已完成 → 67%）

**目标：代码模板层完整，Agent 生成代码有全套参考。**

| # | 内容 | 具体做什么 |
|:--|------|----------|
| F10 | IdempotencyService 模板 | Redis-based tryAcquire + TTL |
| F11 | ExternalApiClient 模板 | 超时+重试+熔断封装 |
| D5 | WireMock stub 示例 | 一个具体的外部 API stub（如 HeyGen API 模拟） |

---

## 第四波：补环境+接线-观测（5 项，✅ 已完成 → 73%）

**目标：环境层可用 + 观测层接入线。**

| # | 内容 | 具体做什么 | 为什么先做 |
|:--|------|----------|------|
| D8 | application-local.yml | 标准化本地开发 profile | 环境缺血肉 |
| D9 | 环境自检脚本 | `docker compose ps` 状态检查 | 和 D8 一样是环境"接线" |
| F14 | 模板参数化 | `com.example` → 实际包名自动替换 | 模板可用性 |
| **E5** | **Micrometer+Prometheus** | actuator 配置 | ⚡ 观测层"接线"——不做的话 E7 Grafana 无处对接 |
| **E6** | **业务指标清单模板** | 按模块列 Counter/Timer/Gauge | ⚡ 和 E5 配套，定义"要观测什么" |



---

## 第五波：补肉-观测（5 项，✅ 已完成 → 79%）

**目标：观测层就绪，生产问题可追踪。**

| # | 内容 | 具体做什么 | 依赖 |
|:--|------|----------|:--:|
| E4 | OpenTelemetry/Tracing | application.yml + OTLP exporter | E5 |
| E7 | Grafana Dashboard JSON | 核心业务 + JVM | E5, E6 |
| E8 | Prometheus 告警规则 | Outbox 积压、错误率、P99 | E5, E6 |
| E9 | Error Catalog→Metrics 联动 | 错误码→Counter | E8 |
| E10 | JFR/Profiling 指引 | 生产定位步骤 | 无 |

---

## 第六波：核心长尾（6 项，✅ 已完成 → 84%）

**目标：直接影响 Agent 输出质量的补齐。**

| 层 | # | 内容 | 说明 | 为什么先做 |
|----|:--|------|------|------|
| C | C10 | 契约测试模板 | Pact / Spring Cloud Contract | 验证跨模块接口——没有契约测试，模块解耦是空话 |
| C | C7 | Checkstyle/SpotBugs/ArchUnit | 编译期规则 | 拦截低级错误，Agent 产出质量的第一道关 |
| B | B5 | Phase 1→business-rules 自动生成 | Agent 从 PRD 提取约束 | 上下文自动生成——不靠人填 |
| B | B6 | Phase 2→project-map 产出 | 架构阶段产出格式化 | 和 B5 配套，打通上下文自动生成链路 |
| B | B7 | Phase 3 自动加载 ai-context/ | Agent 写 Spec 前读取上下文 | "接线"的最后一环——B5/B6 生成的东西终于被消费 |
| C | C9 | 测试数据工厂 | 统一 Test Fixtures | 影响测试编写效率 |

## 第七波：运维长尾（8 项，✅ 已完成 → 100%）

**目标：运维、扩展、文档补齐。** 🎉

| 层 | # | 内容 | 说明 |
|----|:--|------|------|
| B | B8 | Phase 4 扫描 error-catalog | 集成测试遍历错误码 |
| B | B9 | 上下文版本管理 & 过期检测 | project-map 改了 business-rules 没改 → 检测 |
| C | C8 | Annotation Processor 白名单 | Lombok/MapStruct |
| C | C11 | CI 流水线集成 | 本地三层 → CI |
| C | C12 | 反馈时间监控 | 实测 ≤ 3s / ≤ 30s |
| D | D10 | 环境重置脚本 | `docker compose down -v && up -d` |
| D | D11 | Maven (pom.xml) | 对应 build.gradle.kts |
| H | H4 | 新 stack 开发指南 | 如何添加 Quarkus stack |
| H | H5 | CHANGELOG | 变更记录 |

---

## 进度预估

```
当前  ████████████████████████████████████████░░░░░░░░░░░░░░  57%

第一波 █████████████████████████████████████████░░░░░░░░░░░░░  59%  (A8 + H3)
第二波 ███████████████████████████████████████████░░░░░░░░░░░  63%  (+A3, A5, A9)
第三波 ████████████████████████████████████████████░░░░░░░░░░  66%  (+F10, F11, D5)
第四波 ███████████████████████████████████████████████░░░░░░░  72%  (+D8, D9, F14, E5, E6)
第五波 ██████████████████████████████████████████████████░░░░  78%  (+E4, E7, E8, E9, E10)
第六波 █████████████████████████████████████████████████████░  86%  (+C10, C7, B5, B6, B7, C9)
第七波 ██████████████████████████████████████████████████████ 100%  (+8 项运维长尾)
```

---

## 和现有 TODO 的关系

本计划是 TODO.md 的「下一步执行视图」，重新按影响面排序，不改变 TODO 中各项的编号和所属层。
