package com.example.infra.observe;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 错误码 → Prometheus Counter 联动。
 *
 * 每个 error-catalog.yaml 中定义的错误码都对应一个 Counter，
 * 业务代码抛出 BizException 时调用 record() 自动计数。
 *
 * 使用方式：
 *   1. GlobalExceptionHandler 中调用 errorCodeMetrics.record(ex.getCode())
 *   2. Prometheus 自动暴露 app_error_total{code="xxx"} 指标
 *   3. 配合 alert-rules.yml 的告警规则，每个错误码可独立告警
 *
 * 与 .harness/ai-context/error-catalog.yaml 的关系：
 *   - error-catalog.yaml 定义错误码清单（语义）
 *   - ErrorCodeMetrics 暴露每个错误码的 Counter（数据）
 *   - alert-rules.yml 根据 Counter 触发告警（响应）
 */
@Component
public class ErrorCodeMetrics {

    private final MeterRegistry registry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public ErrorCodeMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * 记录一个错误码的触发。
     *
     * 在 GlobalExceptionHandler 中调用：
     * <pre>{@code
     * @ExceptionHandler(BizException.class)
     * public Result<?> handleBizException(BizException ex) {
     *     errorCodeMetrics.record(ex.getCode());
     *     log.warn("Biz error: code={}, message={}", ex.getCode(), ex.getMessage());
     *     return Result.fail(ex.getCode(), ex.getMessage());
     * }
     * }</pre>
     *
     * 生成的 Prometheus 指标：
     *   app_error_total{code="DUPLICATE_REQUEST"}
     *   app_error_total{code="INSUFFICIENT_BALANCE"}
     *   app_error_total{code="DOWNSTREAM_TIMEOUT"}
     *   ...
     */
    public void record(String errorCode) {
        counters.computeIfAbsent(errorCode, code ->
            Counter.builder("app_error_total")
                .description("Total errors by code")
                .tag("code", code)
                .register(registry)
        ).increment();
    }

    /**
     * 记录带附加标签的错误（如按模块区分）。
     *
     * 生成的指标：app_error_total{code="xxx", module="order"}
     */
    public void record(String errorCode, String module) {
        String key = errorCode + ":" + module;
        counters.computeIfAbsent(key, k ->
            Counter.builder("app_error_total")
                .description("Total errors by code and module")
                .tag("code", errorCode)
                .tag("module", module)
                .register(registry)
        ).increment();
    }
}
