package com.example.infra.client;

import java.time.Duration;

/**
 * 外部 API 客户端配置。
 *
 * 每个外部服务一个配置实例，通过子类构造函数传入。
 */
public record ExternalApiConfig(
    String baseUrl,
    Duration connectTimeout,
    Duration readTimeout,
    int maxRetries,
    int circuitBreakerThreshold,
    Duration circuitBreakerWait
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String baseUrl;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(30);
        private int maxRetries = 2;
        private int circuitBreakerThreshold = 5;
        private Duration circuitBreakerWait = Duration.ofSeconds(60);

        public Builder baseUrl(String baseUrl) { this.baseUrl = baseUrl; return this; }
        public Builder connectTimeout(Duration timeout) { this.connectTimeout = timeout; return this; }
        public Builder readTimeout(Duration timeout) { this.readTimeout = timeout; return this; }
        public Builder maxRetries(int retries) { this.maxRetries = retries; return this; }
        public Builder circuitBreakerThreshold(int threshold) { this.circuitBreakerThreshold = threshold; return this; }
        public Builder circuitBreakerWait(Duration wait) { this.circuitBreakerWait = wait; return this; }

        public ExternalApiConfig build() {
            if (baseUrl == null || baseUrl.isBlank()) {
                throw new IllegalArgumentException("baseUrl is required");
            }
            return new ExternalApiConfig(baseUrl, connectTimeout, readTimeout,
                maxRetries, circuitBreakerThreshold, circuitBreakerWait);
        }
    }
}
