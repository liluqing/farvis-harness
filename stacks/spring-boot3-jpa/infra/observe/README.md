# infra/observe/ — 指标实现指南

本目录提供观测层的代码模板。**模板≠即用，需要按项目实现具体指标。**

## 文件说明

| 文件 | 类型 | 做什么 |
|------|------|--------|
| `BusinessMetricsTemplate.java` | 模板 | Micrometer Counter/Timer 的标准写法。复制为 `XxxMetrics.java`，替换 metric 名称和 tag |
| `ErrorCodeMetrics.java` | 可用 | 直接注入 GlobalExceptionHandler。`errorCodeMetrics.record(ex.getCode())` |
| `logback-spring.xml` | 配置 | 结构化 JSON 日志 + MDC traceId。放到 `src/main/resources/` |

## 实现指标的正确姿势

### Step 1：看 `devops/metrics-checklist.yaml`

确定每个模块需要哪些 Counter/Timer/Gauge。例如订单模块：

```yaml
counters:
  - name: order_created_total
  - name: order_duplicate_total
timers:
  - name: order_create_seconds
```

### Step 2：创建 `XxxMetrics.java`

```java
@Component
public class OrderMetrics {
    private final Counter createdCounter;
    private final Counter duplicateCounter;
    private final Timer createTimer;

    public OrderMetrics(MeterRegistry registry) {
        this.createdCounter = Counter.builder("order_created_total")
            .tag("module", "order")
            .register(registry);
        this.duplicateCounter = Counter.builder("order_duplicate_total")
            .tag("module", "order")
            .register(registry);
        this.createTimer = Timer.builder("order_create_seconds")
            .tag("module", "order")
            .register(registry);
    }

    public void recordCreated(Duration duration) {
        createdCounter.increment();
        createTimer.record(duration);
    }

    public void recordDuplicate() {
        duplicateCounter.increment();
    }
}
```

### Step 3：在业务代码中埋点

```java
@Service
public class OrderApplicationService {
    private final OrderMetrics metrics;

    public OrderResult create(OrderCommand cmd) {
        var start = Instant.now();
        // ... 业务逻辑 ...
        metrics.recordCreated(Duration.between(start, Instant.now()));
        return result;
    }
}
```

### Step 4：验证指标暴露

```bash
curl http://localhost:8080/actuator/prometheus | grep order_
# 预期输出：
# order_created_total{module="order"} 42
# order_create_seconds_count{module="order"} 42
```

## 与 Grafana/告警的衔接

指标名称必须与以下文件一致：

| 指标约定来源 | 文件 |
|------------|------|
| 业务指标命名 | `devops/metrics-checklist.yaml` |
| Grafana Dashboard 查询 | `devops/grafana-dashboard.json`（引用 `order_created_total` 等） |
| Prometheus 告警规则 | `devops/alert-rules.yml`（引用 `outbox_pending_count` 等） |

**改指标名 → 三个文件同步改。**

## ExternalApiClient 的指标

`infra/client/ExternalApiClient` 内部定义了 `ApiClientMetrics` 接口。需要提供一个实现类，桥接到 Micrometer：

```java
@Component
public class MicrometerApiClientMetrics implements ApiClientMetrics {
    private final MeterRegistry registry;

    public void recordApiCall(String service, String path, String outcome, Duration duration) {
        Timer.builder("api_call_seconds")
            .tag("service", service).tag("path", path)
            .register(registry).record(duration);
        Counter.builder("api_call_total")
            .tag("service", service).tag("path", path).tag("outcome", outcome)
            .register(registry).increment();
    }
    // ... recordApiFailure, recordCircuitOpen 类似
}
```
