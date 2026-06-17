package com.example.infra.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 外部 API 客户端模板 —— 超时 + 重试 + 熔断 + 降级。
 *
 * 设计原则：
 *  1. 所有外部 HTTP 调用必须经过此模板（不直接使用 RestTemplate）
 *  2. 超时配置在 RestTemplate 层，熔断/重试在本层
 *  3. 每个外部服务一个子类，配置独立的超时/重试/熔断参数
 *
 * 三态熔断器：
 *   CLOSED ──连续失败 N 次──▶ OPEN
 *     ▲                        │
 *     │                        │ 等待 T 秒
 *     └──── 探测成功 ──── HALF_OPEN
 *             探测失败 → 回到 OPEN
 *
 * 使用方式：
 *   // 1. 定义子类
 *   @Component
 *   public class HeyGenClient extends ExternalApiClient {
 *       public HeyGenClient(RestTemplateBuilder builder, BusinessMetricsTemplate metrics) {
 *           super(builder, "heygen", metrics,
 *               ExternalApiConfig.builder()
 *                   .baseUrl("https://api.heygen.com")
 *                   .connectTimeout(Duration.ofSeconds(5))
 *                   .readTimeout(Duration.ofSeconds(30))
 *                   .maxRetries(2)
 *                   .circuitBreakerThreshold(5)
 *                   .circuitBreakerWait(Duration.ofSeconds(60))
 *                   .build());
 *       }
 *   }
 *
 *   // 2. 调用
 *   ApiResponse<VideoStatus> resp = heyGenClient.call(
 *       "/v2/video_status.get",
 *       HttpMethod.GET,
 *       null,  // no request body
 *       VideoStatus.class,
 *       () -> VideoStatus.polling()  // 降级：返回轮询中状态
 *   );
 *
 * @param <T> 响应数据类型
 */
public abstract class ExternalApiClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiClient.class);

    // ====== 熔断状态 ======

    private enum CircuitState { CLOSED, OPEN, HALF_OPEN }

    /** 按服务名隔离的熔断状态（static 因为所有实例共享同一外部服务的熔断状态） */
    private static final Map<String, CircuitState> circuitStates = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> failureCounters = new ConcurrentHashMap<>();
    private static final Map<String, Instant> openTimestamps = new ConcurrentHashMap<>();

    // ====== 实例字段 ======

    private final RestTemplate restTemplate;
    private final String serviceName;
    private final ExternalApiConfig config;
    private final ApiClientMetrics metrics;

    protected ExternalApiClient(
        RestTemplateBuilder builder,
        String serviceName,
        ExternalApiConfig config,
        ApiClientMetrics metrics
    ) {
        this.serviceName = serviceName;
        this.config = config;
        this.metrics = metrics;
        this.restTemplate = builder
            .connectTimeout(config.connectTimeout())
            .readTimeout(config.readTimeout())
            .build();

        // 初始化熔断状态
        circuitStates.putIfAbsent(serviceName, CircuitState.CLOSED);
        failureCounters.putIfAbsent(serviceName, new AtomicInteger(0));
    }

    // ====== 公共 API ======

    /**
     * 调用外部 API，自动处理重试/熔断/降级。
     *
     * @param path           API 路径（如 "/v2/video_status.get"）
     * @param method         HTTP 方法
     * @param requestBody    请求体（GET 时传 null）
     * @param responseType   响应类型
     * @param fallback       降级方案（熔断打开或全部重试失败时调用）
     * @param <T>            响应数据类型
     * @return API 响应或降级结果
     */
    public <T> T call(
        String path,
        HttpMethod method,
        Object requestBody,
        Class<T> responseType,
        Supplier<T> fallback
    ) {
        Instant start = Instant.now();

        // 1. 检查熔断
        if (isCircuitOpen()) {
            log.warn("[{}] Circuit OPEN — executing fallback", serviceName);
            metrics.recordCircuitOpen(serviceName);
            T result = fallback.get();
            metrics.recordApiCall(serviceName, path, "circuit_open", Duration.between(start, Instant.now()));
            return result;
        }

        // 2. 重试循环
        int attempt = 0;
        Exception lastException = null;

        while (attempt <= config.maxRetries()) {
            attempt++;
            try {
                T result = executeHttp(path, method, requestBody, responseType);

                // 成功 → 重置熔断
                onSuccess();
                metrics.recordApiCall(serviceName, path, "success", Duration.between(start, Instant.now()));
                return result;

            } catch (RestClientException e) {
                lastException = e;
                log.warn("[{}] Attempt {}/{} failed: {} {} — {}",
                    serviceName, attempt, config.maxRetries() + 1,
                    method, buildFullUrl(path), e.getMessage());
                metrics.recordApiFailure(serviceName, path, e.getClass().getSimpleName());

                if (attempt <= config.maxRetries()) {
                    sleepBeforeRetry(attempt);
                }
            }
        }

        // 3. 全部重试失败 → 记录失败，检查熔断
        onFailure();
        log.error("[{}] All {} attempts failed for {} {} — executing fallback",
            serviceName, config.maxRetries() + 1, method, buildFullUrl(path), lastException);
        metrics.recordApiCall(serviceName, path, "all_failed", Duration.between(start, Instant.now()));

        return fallback.get();
    }

    // ====== HTTP 执行 ======

    private <T> T executeHttp(String path, HttpMethod method, Object body, Class<T> responseType) {
        HttpEntity<Object> entity = new HttpEntity<>(body, buildHeaders());
        ResponseEntity<T> response = restTemplate.exchange(
            buildFullUrl(path), method, entity, responseType);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RestClientException("HTTP " + response.getStatusCode().value());
        }
        return response.getBody();
    }

    // ====== 熔断逻辑 ======

    private boolean isCircuitOpen() {
        CircuitState state = circuitStates.get(serviceName);
        if (state == CircuitState.OPEN) {
            Instant openedAt = openTimestamps.get(serviceName);
            if (openedAt != null && Duration.between(openedAt, Instant.now()).compareTo(config.circuitBreakerWait()) >= 0) {
                // 等待期满 → 进入半开
                circuitStates.put(serviceName, CircuitState.HALF_OPEN);
                log.info("[{}] Circuit HALF_OPEN — probing", serviceName);
                return false;  // 允许通过这次探测请求
            }
            return true;  // 还在等待期，拒绝
        }
        return false;  // CLOSED 或 HALF_OPEN，允许通过
    }

    private void onSuccess() {
        circuitStates.put(serviceName, CircuitState.CLOSED);
        failureCounters.get(serviceName).set(0);
    }

    private void onFailure() {
        int failures = failureCounters.get(serviceName).incrementAndGet();
        if (failures >= config.circuitBreakerThreshold()) {
            circuitStates.put(serviceName, CircuitState.OPEN);
            openTimestamps.put(serviceName, Instant.now());
            log.warn("[{}] Circuit OPEN — {} consecutive failures, wait {}s",
                serviceName, failures, config.circuitBreakerWait().toSeconds());
        }
    }

    // ====== 辅助方法 ======

    private String buildFullUrl(String path) {
        return config.baseUrl() + path;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 子类可覆盖 addHeaders() 添加认证头
        addHeaders(headers);
        return headers;
    }

    /** 子类覆盖此方法添加自定义请求头（如 Authorization） */
    protected void addHeaders(HttpHeaders headers) {
        // 默认空实现
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            // 指数退避：1s, 2s, 4s...
            long delayMs = (long) Math.pow(2, attempt - 1) * 1000;
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ====== 监控 ======

    /**
     * API 客户端指标接口 —— 记录调用次数/耗时/熔断事件。
     *
     * 实现类可桥接到 infra/observe/BusinessMetricsTemplate 的 Micrometer Counter/Timer。
     */
    public interface ApiClientMetrics {
        void recordApiCall(String service, String path, String outcome, Duration duration);
        void recordApiFailure(String service, String path, String errorType);
        void recordCircuitOpen(String service);
    }

    /**
     * RestTemplate 构建器 —— 隔离 RestTemplate 的创建细节。
     */
    public interface RestTemplateBuilder {
        RestTemplateBuilder connectTimeout(Duration timeout);
        RestTemplateBuilder readTimeout(Duration timeout);
        RestTemplate build();
    }
}
