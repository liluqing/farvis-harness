# 新 Stack 开发指南

如何为 Harness 添加新的技术栈（如 Quarkus、Micronaut、MyBatis 替代 JPA）。

## 三步法

### Step 1：复制骨架

```bash
cp -r stacks/spring-boot3-jpa/ stacks/quarkus-panache/
```

### Step 2：替换框架特化

| 目录/文件 | 原始内容（Spring Boot） | 替换为（新栈） |
|----------|----------------------|-------------|
| `devops/docker-compose.yml` | 通常不变 | 如需其他中间件（如 PostgreSQL 代替 MySQL）修改 |
| `devops/build.gradle.kts` | Spring Boot 插件 | 新框架的构建配置 |
| `devops/pom.xml` | Spring Boot parent | 新框架的 BOM |
| `devops/application-*.yml` | `spring.*` 属性 | 新框架的配置键 |
| `infra/exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` | 新框架的异常处理机制 |
| `infra/slice/ModuleSliceTestConfiguration.java` | `@SpringBootTest` | 新框架的测试注解 |
| `templates/*.java` | `@Service`/`@RestController` | 新框架的等价注解 |
| `coding-rules.yaml` | Spring Boot 特化规则 | 新框架的特化规则 |

### Step 3：保留框架无关

**不要改这些——它们是跨框架通用的：**

- `core/` 下的所有文件（principles/patterns/ai-context 模板）
- `.harness/skills/harness-java.md`（运行时 Skill 是 Java 通用的）
- `SKILL.md`（初始化 Skill 通过 stack 选择支持多栈）
- `infra/outbox/` — Outbox 模式是模式，实现可以不同，但概念不变
- `infra/idempotency/` — Redis SETNX 与框架无关
- `infra/observe/` — Micrometer 是 JVM 生态通用
- `examples/farvis-ai/` — 填写示例，可新增新栈的示例

## 验收标准

新 stack 必须通过以下检查才算完成：

1. [ ] SKILL.md 的 STEP 2（选择 stack）能自动检测新栈
2. [ ] `harness-java-init` 初始化新栈项目后，Agent 能正常加载 runtime Skill
3. [ ] 新栈的 `infra/` 代码能编译通过
4. [ ] 新栈的 `templates/` 在 Phase 3 能被 Agent 正确引用
5. [ ] 新栈的 `coding-rules.yaml` 与 core 的 `coding-rules.yaml` 无冲突规则
6. [ ] README.md 更新（在 stacks/ 总览中加入新栈）

## 示例：添加 Quarkus + Panache

```bash
# Step 1
cp -r stacks/spring-boot3-jpa/ stacks/quarkus-panache/

# Step 2：替换关键文件
# build.gradle.kts → 改为 Quarkus 插件 + Panache 依赖
# application*.yml → 改为 quarkus.* 配置前缀
# GlobalExceptionHandler → 改为 JAX-RS ExceptionMapper
# 模板注解 → @Inject 代替 @Autowired, @Path 代替 @RequestMapping
# coding-rules.yaml → Quarkus 特化规则（如禁止 @Autowired 字段注入）

# Step 3：不动
# core/、.harness/、SKILL.md、infra/outbox/、infra/idempotency/ 不动
```
