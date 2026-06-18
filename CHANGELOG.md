# CHANGELOG

## [0.3.1] - 2026-06-18

### Fixed（开发者体验闭环修复）
- 🔴 **SKILL.md cp -r 嵌套目录**：STEP 3.7/3.9/3.10/3.11 全部改用 `/*` 避免嵌套（devops/devops/、templates/templates/、scripts/scripts/、principles/principles/、patterns/patterns/）
- 🔴 **application-test.yml ddl-auto**：`validate` → `create-drop`，Testcontainers 空库可自动建表
- 🔴 **init-sql DATETIME→DATETIME(6)**：兼容 Hibernate 6 Instant 映射
- 🔴 **pre-commit hook fastTest fallback**：fastTest task 不存在时 warn 跳过而非 block
- 🔴 **OutboxDispatcher 空跑保护**：未配置消息队列时 dispatch() 跳过 + WARN
- 🟡 **BusinessMetricsTemplate Counter 重复注册**：`recordWithStatus()` 改用 ConcurrentHashMap 缓存
- 🟡 **service-template 类名**：`ResourceMetrics` → `BusinessMetricsTemplate`，`recordDuplicate()` → `recordIdempotencyHit()`
- 🟡 **harness-java.md 文件名**：`coding-rules-spring-boot.yaml` → `coding-rules-spring-boot3-jpa.yaml`
- 🟢 **SKILL.md cp -n→--no-clobber**：消除 GNU cp warning
- 🟢 **SKILL.md AGENTS.md 重复追加**：STEP 5.2 增加「## Harness」已存在则跳过
- 🟡 **pre-commit/pre-push hook 路径解析**：`$0` 改用 `readlink -f` 解析 symlink，避免 `.git/hooks/` vs `.harness/hooks/git/` 路径歧义

## [0.3.0] - 2026-06-17

### Added
- **第七波（运维长尾）**：Phase 3 环境自检（A10）、上下文版本检测脚本（B9）、AP 白名单（C8）、GitHub Actions CI（C11）、反馈时间监控（C12）、Maven pom.xml（D11）、OpenTelemetry 配置（E4）、Resilience4j 配置（F12）、新 stack 开发指南（H4）、CHANGELOG（H5）
- **第六波（核心长尾）**：Phase 1→business-rules 结构化提取 + error-catalog 同步（B5）、Phase 2→project-map 产出（B6）、Phase 3 自动加载 ai-context（B7）、Phase 4 扫描 error-catalog（B8）、Checkstyle/SpotBugs（C7）、测试数据工厂（C9）、契约测试模板（C10）
- **第五波（观测补肉）**：Grafana Dashboard JSON（E7）、Prometheus 告警规则（E8）、Error→Metrics 联动（E9）、JFR Profiling 指引（E10）、环境重置脚本（D10）
- **第四波（补环境+接线）**：application-local.yml（D8）、env-check.sh（D9）、parameterize.py（F14）、actuator 配置（E5）、业务指标清单（E6）
- **第三波（补代码模式）**：IdempotencyService（F10）、ExternalApiClient（F11）、WireMock stub 示例（D5）
- **第二波（补流程）**：Phase 2/Phase 4 flow.md 增强、独立 TDD 五步文档（A9）
- **第一波（接线）**：Phase 1→4 flow.md 全部接入 `.harness/`

### Fixed
- Round 1 Review（5 个修复）：SKILL.md 缺失文件、命名冲突、gradle 路径、速查表过时
- Round 2 Review（5 个修复）：Phase 1→4 error-catalog 数据流、scripts/ 复制、B8 同步、contract property、observe README
- Final Review（4 个修复）：README 版本号 0.1.0→0.3.0、SKILL.md 版本号 0.2.0→0.3.0、Phase 3 加 `./gradlew fastTest` 命令、Phase 4 加 env-check.sh 调用

### Changed
- SKILL.md STEP 3.7：从逐个列文件改为 `cp -r devops/` + `rm build.gradle.kts`
- coding-rules.yaml：5 条规则补 infra 实现路径注释
- harness-java.md：gradle 命令修正、模式速查表 8 行、TDD 引用
- stacks/spring-boot3-jpa/README.md：完全重写

---

## [0.1.0] - 2026-06-16

### Added
- 初始框架：六层模型（A~F + G Skill + H 跨层）
- core/：框架无关原理（5 条 + 4 个模式）+ 4 个 ai-context 空模板
- stacks/spring-boot3-jpa/：Spring Boot 3 + JPA 特化实现
  - devops：docker-compose、build.gradle.kts、application-test.yml、init-sql
  - infra：outbox/、slice/、observe/、exception/、arch/
  - templates：controller/service/repository/entity/dto + 三层测试
- .harness/skills/harness-java.md：运行时 Skill
- SKILL.md：初始化 Skill（detect→copy→symlink→AGENTS.md）
- examples/farvis-ai/：填写示例
- Sub-agent 端到端验证通过（43 文件就位、软链接可解析、hooks 有权限）
