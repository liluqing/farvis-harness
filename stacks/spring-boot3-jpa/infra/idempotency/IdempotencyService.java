package com.example.infra.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * 幂等服务 —— 基于 Redis SETNX 的请求去重。
 *
 * 核心思路：
 *  1. 客户端传入唯一 requestId
 *  2. tryAcquire 用 SETNX 原子抢占幂等键
 *  3. 抢占成功 → 执行业务，完成后 markComplete（写结果）
 *  4. 抢占失败 → 说明是重复请求，getResult 返回已有结果
 *
 * TTL 自动过期：幂等键存活 TTL 时间后自动清理，无需手动释放。
 *
 * 使用方式：
 *   String key = "order:create:" + command.requestId();
 *   if (!idempotencyService.tryAcquire(key, Duration.ofMinutes(10))) {
 *       return idempotencyService.getResult(key, OrderResult.class);
 *   }
 *   // ... 执行业务 ...
 *   idempotencyService.markComplete(key, result);
 */
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    /** Redis key 前缀，避免与其他业务键冲突 */
    private static final String KEY_PREFIX = "idempotency:";

    /** 幂等键的 TTL 建议值：10 分钟（通常覆盖重试窗口） */
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final IdempotencyResultSerializer serializer;

    public IdempotencyService(StringRedisTemplate redis, IdempotencyResultSerializer serializer) {
        this.redis = redis;
        this.serializer = serializer;
    }

    /**
     * 尝试获取幂等锁。
     *
     * @param idempotencyKey 幂等键（约定格式："{业务域}:{操作}:{requestId}"）
     * @param ttl            幂等键存活时间，超过后自动清理
     * @return true = 首次请求，执行业务；false = 重复请求，走幂等返回
     */
    public boolean tryAcquire(String idempotencyKey, Duration ttl) {
        String fullKey = KEY_PREFIX + idempotencyKey;
        Boolean acquired = redis.opsForValue()
            .setIfAbsent(fullKey, "ACQUIRED", ttl);

        boolean result = Boolean.TRUE.equals(acquired);
        if (result) {
            log.debug("Idempotency acquired: key={}, ttl={}", idempotencyKey, ttl);
        } else {
            log.info("Idempotency hit (duplicate): key={}", idempotencyKey);
        }
        return result;
    }

    /**
     * 标记业务完成，写入结果。
     *
     * 注意：只有在 tryAcquire 成功后才调用。结果写入后，后续重复请求
     * 可以通过 getResult 直接拿到这个结果。
     */
    public void markComplete(String idempotencyKey, Object result) {
        String fullKey = KEY_PREFIX + idempotencyKey;
        String json = serializer.serialize(result);
        // 覆盖原值，TTL 从当前时间重新算
        Duration ttl = redis.getExpire(fullKey);
        if (ttl == null || ttl.isNegative()) {
            ttl = DEFAULT_TTL;
        }
        redis.opsForValue().set(fullKey, json, ttl);
        log.debug("Idempotency completed: key={}", idempotencyKey);
    }

    /**
     * 标记业务失败，释放幂等键允许重试。
     *
     * 用于业务处理异常但客户端可以重试的场景（如：库存不足后补充了库存）。
     * 如果是不该重试的错误（如参数校验失败），不要调用此方法——让幂等键自动过期。
     */
    public void markFailed(String idempotencyKey) {
        String fullKey = KEY_PREFIX + idempotencyKey;
        redis.delete(fullKey);
        log.info("Idempotency released for retry: key={}", idempotencyKey);
    }

    /**
     * 获取幂等结果（重复请求时调用）。
     *
     * @return 已有结果，如果幂等键已过期或无结果返回 Optional.empty()
     */
    public <T> Optional<T> getResult(String idempotencyKey, Class<T> type) {
        String fullKey = KEY_PREFIX + idempotencyKey;
        String json = redis.opsForValue().get(fullKey);
        if (json == null || "ACQUIRED".equals(json)) {
            // 幂等键存在但业务还没完成（ACQUIRED 是 tryAcquire 的占位值）
            // 或者已过期
            return Optional.empty();
        }
        return Optional.ofNullable(serializer.deserialize(json, type));
    }

    /**
     * 检查幂等键是否存在（用于监控/排查）。
     */
    public boolean exists(String idempotencyKey) {
        String fullKey = KEY_PREFIX + idempotencyKey;
        return Boolean.TRUE.equals(redis.hasKey(fullKey));
    }
}
