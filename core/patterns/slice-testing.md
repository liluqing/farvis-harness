# 切片测试模式

## 问题

Java 项目的单元测试可以很快（Mock 一切），但集成测试需要启动整个 Spring 容器——30 秒起步。

AI 需要高频验证（改一行 → 3 秒内知道对错），完整启动是不可接受的。

## 模式

**只启动被测模块所需的 Bean，不启动完整应用。**

```
完整启动：所有 Controller + Service + Repository + 配置 + 中间件 → 30s+
切片测试：只加载当前模块的 Service + 必要的自动配置 → ≤ 3s
```

## 如何切割

按业务模块切，不按技术层切：

```
✅ UserSlice: UserController + UserService + UserRepository
❌ ControllerSlice: 所有 Controller（跨业务域，无意义）
```

每个切片的 Spring 配置：

```
@SpringBootConfiguration
@EnableAutoConfiguration
@Import({
    UserApplicationService.class,   // 只导入这个模块的 Bean
    UserDomainService.class,
    ValidationAutoConfiguration.class
})
public class UserSliceTestConfiguration {}
```

## 关键决策

| 决策点 | 选择 | 说明 |
|--------|------|------|
| 切面粒度 | 按业务模块 | 一个模块一个切片 |
| 外部依赖 | WireMock 模拟 | 不依赖真实外部 API |
| 数据库 | H2 内存库 / Testcontainers | 简单查询用 H2，复杂 SQL 用 Testcontainers |
| 测试时间上限 | ≤ 30s | 超过 30s 应降级为集成测试 |

## 与技术栈无关的约束

- 每个模块必须有一个切片测试配置
- 切片测试不能依赖外部 API（必须 Mock）
- 切片测试时间必须在 30s 以内
- 完整启动不是默认验证方式
