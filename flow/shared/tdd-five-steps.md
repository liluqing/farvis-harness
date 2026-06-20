# TDD 五步循环

> **定位：** Phase 3 开发的核心执行流程。每个切片走完这五步才算完成。
> **适用：** Java 项目（已接入 Harness 时走增强版，未接入走标准版）
> **引用者：** Phase 3 flow.md 的阶段 ④~⑧

---

## 何时走五步 vs 三步

| 条件 | 走五步 | 走三步（标准 TDD） |
|------|:--:|:--:|
| 项目有 `.harness/templates/` | ✅ | — |
| 项目有 `docker compose` 且可启动 | ✅ | — |
| 简单 CRUD 无外部依赖 | ②④ 可跳过 | ✅ |
| 纯工具类 / 静态方法 | ② 只走单元测试 | ✅ |

> **Agent 自判：** 检查 `.harness/` 是否存在 → 存在走五步，否则退化为三步。
> 三步版：① 写测试 → ② 跑测试（RED）→ ③ 编码（GREEN）→ 自验

---

## 流程总览

```
切片任务（从 Phase 2 任务清单 或 PRD 拆出）
    ↓
┌─────────────────────────────────────────────┐
│ ① 编译 + 静态规则检查          ≤ 3s         │
│    → 失败 → 修复 → 重试                     │
├─────────────────────────────────────────────┤
│ ② 单元测试（Mock 外部依赖）                 │
│    → RED → 进入实现                         │
├─────────────────────────────────────────────┤
│ ③ 模块切片测试（Testcontainers 按需）≤ 30s │
│    → 涉及 Spring Bean 装配时必写            │
├─────────────────────────────────────────────┤
│ ④ 集成测试（按需触发）                      │
│    → 涉及 DB/缓存/外部 API 时必写           │
├─────────────────────────────────────────────┤
│ ⑤ REFACTOR                                  │
│    → 测试全绿后再改结构                      │
└─────────────────────────────────────────────┘
    ↓
自验（Phase 3 阶段 ④）
```

---

## ① 编译 + 静态规则检查

**目的：** 确保代码在语法和编码规范层面是正确的，不等测试就快速失败。

### 入口条件
- 代码已写入（从模板生成或手写）
- 依赖已声明（`build.gradle.kts` / `pom.xml`）

### 执行命令

```bash
# Gradle（推荐）
./gradlew compileJava       # 增量编译，≤ 3s

# 如有 Checkstyle / SpotBugs（Harness 已配置）
./gradlew checkstyleMain spotbugsMain
```

### 出口条件
- [ ] `compileJava` 零错误零警告
- [ ] 如有静态分析 → 零违规（warning 级别按项目约定）

### 跳过规则

| 情况 | 处理 |
|------|------|
| 项目无静态分析配置 | 跳到下一步，标注 `[无静态分析]` |
| 修改仅涉及测试文件 | 跑 `compileTestJava` |

### 常见失败及处理

| 失败 | 原因 | 修复 |
|------|------|------|
| `package xxx does not exist` | 缺少依赖 | 检查 `build.gradle.kts`，加 `implementation()` |
| `cannot find symbol` | 类名/方法名写错 | 检查拼写、import |
| Checkstyle 违规 | 格式不规范 | 按规则自动修复（IDE format） |
| SpotBugs 警告 | 潜在 bug | 评估是真 bug 还是误报，真 bug 修复 |

---

## ② 单元测试

**目的：** 验证纯逻辑层的正确性。Mock 所有外部依赖（DB、HTTP、MQ）。

### 入口条件
- [ ] 编译通过

### 编写测试

```java
// src/test/java/<base-pkg>/<layer>/<ClassName>Test.java

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_shouldAssignOrderId_onValidRequest() {
        // Given
        CreateOrderRequest req = new CreateOrderRequest("SKU-001", 2);
        when(idempotencyService.tryAcquire(any())).thenReturn(true);
        when(orderRepository.save(any())).thenReturn(mockOrder);

        // When
        OrderDTO result = orderService.createOrder(req);

        // Then
        assertThat(result.getOrderId()).isNotNull();
        verify(orderRepository).save(any());
    }

    @Test
    void createOrder_shouldThrow_onDuplicate() {
        // Given
        when(idempotencyService.tryAcquire(any())).thenReturn(false);

        // When & Then
        assertThrows(DuplicateRequestException.class,
            () -> orderService.createOrder(req));
    }
}
```

### 执行命令

```bash
./gradlew test --tests "*Test"      # 只跑单元测试（命名约定：*Test.java）
```

### 出口条件
- [ ] **RED 阶段**：测试失败原因是「代码未实现」（不是测试写错）
- [ ] **GREEN 阶段**：所有单元测试通过，覆盖率 ≥ 80%（核心逻辑）

### 跳过规则

| 情况 | 处理 |
|------|------|
| 切片只有 Controller 层（无业务逻辑） | 跳到③切片测试 |
| 切片是纯 DTO / Entity（无方法） | 跳过，标注 `[无逻辑]` |
| RED 失败原因是测试写错 | 先修测试，再走 RED |

---

## ③ 模块切片测试

**目的：** 验证 Spring Bean 装配 + 本模块的 HTTP/DB 集成，不启动无关模块。

### 入口条件
- [ ] 单元测试 GREEN
- [ ] 切片涉及 Spring Bean（Controller / Service / Repository）

### 编写测试

```java
// src/test/java/<base-pkg>/<module>/slice/<ClassName>SliceTest.java

@WebMvcTest(OrderController.class)           // 只装配 Controller 层
@Import({OrderService.class, IdempotencyService.class})  // 只导入需要的 Bean
class OrderControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderRepository orderRepository; // 其他 Bean 全部 Mock

    @Test
    void createOrder_shouldReturn201_onValidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"skuCode": "SKU-001", "quantity": 2}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").isNotEmpty());
    }

    @Test
    void createOrder_shouldReturn409_onDuplicate() throws Exception {
        // ...
    }
}
```

```java
// 如涉及 DB 操作，切片测试也可用 @DataJpaTest
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositorySliceTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void findByOrderId_shouldReturnOrder_whenExists() {
        // Given: 用 Testcontainers 提供的真实 PostgreSQL
        // ...
    }
}
```

### 执行命令

```bash
./gradlew test --tests "*SliceTest"  # 只跑切片测试，≤ 30s
```

### 出口条件
- [ ] 切片测试全部 GREEN
- [ ] 耗时 ≤ 30s（如果超时，检查是否启动了无关 Bean）

### 跳过规则

| 情况 | 处理 |
|------|------|
| 切片不涉及 Spring Bean | 跳过，标注 `[无 Bean]` |
| 已有同模块 SliceTest 覆盖 | 增量追加用例，不新建文件 |
| 项目不是 Spring 框架 | 跳过，标注 `[非 Spring]` |

---

## ④ 集成测试

**目的：** 验证本切片与真实外部依赖（DB、缓存、外部 API）的交互。

### 入口条件
- [ ] 切片测试 GREEN
- [ ] 切片涉及 DB 写入 / 缓存操作 / 外部 API 调用 → 必须写
- [ ] `docker compose up -d` 基础设施就绪

### 编写测试

```java
// src/test/java/<base-pkg>/<module>/integration/<ClassName>IntegrationTest.java

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)          // 自动启动 WireMock
@Testcontainers                            // 自动启动 Testcontainers
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createOrder_shouldPersistAndReturnOrder() throws Exception {
        // Given: WireMock stub 外部 API（如有）
        // stubFor(...)

        // When
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"skuCode": "SKU-001", "quantity": 2}"""))
            .andExpect(status().isCreated());

        // Then: 验证 DB 数据
        // 用 repository 直接查 DB
    }
}
```

### 执行命令

```bash
./gradlew integrationTest  # 专用 task，慢但完整
# 或
./gradlew test --tests "*IntegrationTest"  # 按命名约定
```

### 出口条件
- [ ] 集成测试 GREEN
- [ ] 所有外部依赖（DB/缓存/API）交互正确
- [ ] 如涉及 Outbox → 消息已入库

### 跳过规则

| 情况 | 处理 |
|------|------|
| 切片纯计算/转换，无外部依赖 | 跳过，标注 `[无外部依赖]` |
| 环境不可用（docker compose 未启动） | 提示用户启动，标注 `[环境不可用]` |
| 用户明确说「不用集成测试」 | 听用户的，标注 `[用户跳过]` |

---

## ⑤ REFACTOR

**目的：** 测试全绿后优化代码结构，消除重复，提升可读性。**不动行为，只改结构。**

### 入口条件
- [ ] ①~④ 全部 GREEN
- [ ] 自验通过（Phase 3 阶段 ⑨）

### 检查清单

- [ ] 是否有重复代码（同逻辑出现 ≥ 2 次）→ 提取方法/类
- [ ] 方法是否过长（> 30 行）→ 拆分子方法
- [ ] 命名是否准确（变量/方法/类名是否表意）→ 重命名
- [ ] 是否有不必要的 null 检查 → 用 Optional 或 @NonNull
- [ ] 异常处理是否一致 → 统一走 GlobalExceptionHandler
- [ ] 日志级别是否合理（info/debug/warn/error）

### 执行命令

```bash
./gradlew fastTest  # 重构后跑快速测试验证没搞坏
```

### 出口条件
- [ ] 重构后 ①~④ 仍然全部 GREEN
- [ ] 代码可读性：新加入的同事能看懂核心流程
- [ ] 无新增 TODO / FIXME（除非有意标注）

### 不做的事（反模式）

- ❌ 「顺便」改 PRD 范围外的功能
- ❌ 「优化」性能没有基准测试做依据
- ❌ 「统一风格」改动超过本切片的文件

---

## 决策矩阵（Agent 自判）

| 切片类型 | ① 编译 | ② 单元 | ③ 切片 | ④ 集成 | ⑤ 重构 |
|---------|:--:|:--:|:--:|:--:|:--:|
| Controller + Service + DB | ✅ | ✅ | ✅ | ✅ | ✅ |
| Controller 纯转发 | ✅ | ❌ | ✅ | ✅ | ✅ |
| Service 纯计算（无 DB） | ✅ | ✅ | ✅ | ❌ | ✅ |
| Entity / DTO（无逻辑） | ✅ | ❌ | ❌ | ❌ | ❌ |
| 工具类 / 静态方法 | ✅ | ✅ | ❌ | ❌ | ✅ |
| 配置类 | ✅ | ❌ | ✅ | ❌ | ❌ |
| 异常类 / 枚举 | ✅ | ❌ | ❌ | ❌ | ❌ |

---

## TDD 铁律

1. **必须先 RED 再实现**——不跳过失败阶段
2. **RED 失败原因只能是「代码未实现」**——测试写错了先修测试
3. **GREEN 不通过只改代码，不改测试**——测试是契约
4. **每步完成通报进度**——告诉用户「② 单元测试 GREEN，进入③切片测试」
5. **任一步失败超过 3 次**——停下来，分析根因，不要盲目重试
