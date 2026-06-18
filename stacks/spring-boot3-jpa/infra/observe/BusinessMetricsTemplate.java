package com.example.infra.observe;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 业务指标模板。
 *
 * 命名规范：{project}_{module}_{operation}_{unit}
 *   例：order_service_order_created_total
 *       order_service_order_create_latency_seconds
 *
 * Counter：只增不减的计数（请求数、成功数、失败数、幂等命中数）
 * Timer：耗时分布（P50/P95/P99）
 * Gauge：瞬时值（队列长度、连接池大小）
 *
 * 使用方式：
 *   1. 按模块创建 XxxMetrics 类
 *   2. 在业务方法中调用 record*() 方法
 *   3. Prometheus 自动抓取 /actuator/prometheus
 */
@Component
public class BusinessMetricsTemplate {

    // ====== Counter 示例 ======

    /** 操作成功次数 */
    private final Counter successCounter;

    /** 操作失败次数 */
    private final Counter failureCounter;

    /** 幂等命中次数 */
    private final Counter idempotencyHitCounter;

    // ====== Timer 示例 ======

    /** 操作耗时 */
    private final Timer operationTimer;

    public BusinessMetricsTemplate(MeterRegistry registry) {
        this.successCounter = Counter.builder("app_operation_success_total")
            .description("Total successful operations")
            .tag("module", "template")
            .register(registry);

        this.failureCounter = Counter.builder("app_operation_failure_total")
            .description("Total failed operations")
            .tag("module", "template")
            .register(registry);

        this.idempotencyHitCounter = Counter.builder("app_idempotency_hit_total")
            .description("Total idempotency hits")
            .tag("module", "template")
            .register(registry);

        this.operationTimer = Timer.builder("app_operation_latency_seconds")
            .description("Operation latency in seconds")
            .tag("module", "template")
            .register(registry);
    }

    // ====== 记录方法 ======

    public void recordSuccess(Duration duration) {
        successCounter.increment();
        operationTimer.record(duration);
    }

    public void recordFailure() {
        failureCounter.increment();
    }

    public void recordIdempotencyHit() {
        idempotencyHitCounter.increment();
    }

    // ====== 带自定义标签的 Counter（按需扩展）==========

    /** 缓存按标签区分的 Counter，避免重复注册 */
    private final java.util.concurrent.ConcurrentHashMap<String, Counter> taggedCounters = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 按状态记录（如：success / failed / processing）。
     * 内部缓存 Counter 避免重复注册同名 meter。
     */
    public void recordWithStatus(MeterRegistry registry, String metricName, String status) {
        String cacheKey = metricName + "::" + status;
        taggedCounters.computeIfAbsent(cacheKey, k ->
            Counter.builder(metricName)
                .tag("status", status)
                .register(registry)
        ).increment();
    }
}
