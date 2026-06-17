# Stack：Spring Boot 3 + JPA

此目录提供 **Spring Boot 3.x + Spring Data JPA + MySQL + Redis** 技术栈的 Harness 具体实现。

## 适用条件

- Java 17+
- Spring Boot 3.x
- Spring Data JPA（Hibernate）
- MySQL 8.0
- Redis 7.2
- Gradle 8.x（Kotlin DSL）
- Docker Compose 本地环境

## 目录

```
stacks/spring-boot3-jpa/
├── README.md
├── coding-rules.yaml                # 框架特化编码规则（补充 core 通用规则）
├── scripts/
│   └── parameterize.py             # 模板参数化：com.example → 实际包名
├── devops/
│   ├── docker-compose.yml          # MySQL + Redis + WireMock
│   ├── build.gradle.kts            # 增量编译 + fastTest/integrationTest 分层
│   ├── application-test.yml        # Testcontainers 动态数据源 + Redis + WireMock
│   ├── application-local.yml       # 本地开发 profile（真实 MySQL/Redis + actuator）
│   ├── application-actuator.yml    # 独立 actuator 配置片段（/actuator/prometheus）
│   ├── prometheus.yml              # 本地 Prometheus 抓取配置
│   ├── metrics-checklist.yaml      # 业务指标清单模板（按模块列 Counter/Timer/Gauge）
│   ├── env-check.sh                # 环境自检脚本（docker compose 状态 + DB/Redis/WireMock）
│   ├── init-sql/                   # 建表 DDL
│   └── wiremock/
│       └── stubs/
│           └── heygen-api.json     # 外部 API 模拟示例（7 场景）
├── infra/
│   ├── arch/
│   │   └── LayeredArchitectureTest.java    # ArchUnit 分层校验规则
│   ├── client/
│   │   ├── ExternalApiClient.java          # 外部 API 客户端模板（超时+重试+熔断+降级）
│   │   └── ExternalApiConfig.java          # Builder 模式配置
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java     # 统一异常处理（@RestControllerAdvice）
│   │   └── BizException.java              # 业务异常基类（含错误码）
│   ├── idempotency/
│   │   ├── IdempotencyService.java         # Redis SETNX + TTL 幂等服务
│   │   └── IdempotencyResultSerializer.java # 幂等结果序列化接口
│   ├── observe/
│   │   ├── logback-spring.xml              # 结构化 JSON 日志（含 traceId）
│   │   └── BusinessMetricsTemplate.java    # Micrometer Counter + Timer 模板
│   ├── outbox/
│   │   ├── OutboxEvent.java                # JPA 实体（含状态方法）
│   │   ├── OutboxEventRepository.java      # Spring Data JPA
│   │   └── OutboxDispatcher.java           # @Scheduled 投递器
│   ├── slice/
│   │   └── ModuleSliceTestConfiguration.java  # 切片测试基类
│   └── Result.java                         # 统一返回体 {code, data, message}
└── templates/
    ├── controller-template.java
    ├── service-template.java
    ├── repository-template.java
    ├── entity-template.java
    ├── dto-template.java
    └── test/
        ├── unit-test-template.java
        ├── slice-test-template.java
        └── integration-test-template.java
```

## 核心实现在 core/ 的哪些原理

| core 原理/模式 | stack 实现位置 |
|--------------|---------------|
| 01-fast-feedback | `devops/build.gradle.kts`（fastTest ≤ 3s）、`infra/slice/` |
| 02-context-contract | `coding-rules.yaml`、`core/ai-context/` 四文件 |
| 03-auto-verification | `templates/test/` 三层测试模板、`infra/arch/` |
| 04-env-consistency | `devops/docker-compose.yml`、`application-test.yml`、`application-local.yml` |
| 05-observability | `infra/observe/`、`application-actuator.yml`、`metrics-checklist.yaml` |
| outbox | `infra/outbox/` |
| idempotency | `infra/idempotency/` |
| circuit-breaker | `infra/client/` |
| slice-testing | `infra/slice/` |

## 初始化到项目

初始化由 `SKILL.md`（harness-java-init）自动执行。关键步骤：

1. 复制 `devops/` 下所有文件到 `.harness/devops/`（含 docker-compose、配置、脚本、stubs）
2. 复制 `infra/` 下所有文件到 `.harness/infra/`
3. 复制 `templates/` 到 `.harness/templates/`
4. 复制 `coding-rules.yaml` 到 `.harness/ai-context/coding-rules-spring-boot3-jpa.yaml`
5. 运行 `scripts/parameterize.py` 替换 `com.example` → 实际包名

> build.gradle.kts 不复制——配置作为「建议配置」写入 AGENTS.md，由开发者决定是否合并。

## 已知局限

- 仅支持 Gradle（Maven 版本待补充）
- 仅支持 JPA（MyBatis 版本待补充）
- 消息队列部分为占位（Kafka/RabbitMQ 发送代码需手动替换）
