# Core — Harness 原理层

此目录定义 Harness 的**框架无关原理**。无论项目用 Spring Boot、Quarkus、Micronaut 还是纯 Servlet，这些原理都适用。

## 目录

```
core/
├── README.md
├── principles/          # 五条方法论（与技术栈无关）
│   ├── 01-fast-feedback.md
│   ├── 02-context-contract.md
│   ├── 03-auto-verification.md
│   ├── 04-env-consistency.md
│   └── 05-observability.md
├── patterns/            # 通用设计模式（概念层，不绑框架）
│   ├── outbox.md
│   ├── idempotency.md
│   ├── circuit-breaker.md
│   └── slice-testing.md
├── ai-context/          # 上下文结构定义（YAML schema，项目填写）
│   ├── project-map.yaml
│   ├── business-rules.yaml
│   ├── error-catalog.yaml
│   └── coding-rules.yaml
└── devops/              # 通用环境定义（占位，具体由 stack 覆盖）
    └── docker-compose.yml  # 只定义结构，不含具体镜像版本
```

## 与 stacks/ 的关系

```
core/principles/          → 告诉 Agent "要做什么"
core/patterns/            → 告诉 Agent "怎么做（概念上）"
stacks/{stack}/           → 告诉 Agent "具体代码怎么写"
```

Agent 初始化 Harness 时：读 core/ 理解原理 → 按项目技术栈选 stack/ → 复制对应模板到目标项目。
