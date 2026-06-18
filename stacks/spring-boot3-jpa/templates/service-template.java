package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application Service 模板 —— 跨能力编排层。
 *
 * 约定：
 *  1. 只做跨能力编排，不包含领域逻辑
 *  2. 幂等检查在最前面
 *  3. 外部 API 调用通过专用 Client
 *  4. 涉及 DB + 消息时使用 Outbox 模式
 *  5. 使用 @Transactional 明确事务边界
 *
 * 使用方式：Phase 3 切片的 Service 层按此模板生成。
 */
@Service
public class ResourceApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ResourceApplicationService.class);

    private final IdempotencyService idempotencyService;
    private final ResourceDomainService domainService;
    private final BusinessMetricsTemplate metrics;

    public ResourceApplicationService(
        IdempotencyService idempotencyService,
        ResourceDomainService domainService,
        BusinessMetricsTemplate metrics
    ) {
        this.idempotencyService = idempotencyService;
        this.domainService = domainService;
        this.metrics = metrics;
    }

    /**
     * 核心业务流程。
     *
     * 步骤：
     *  1. 幂等检查（防重复提交）
     *  2. 业务校验（参数/状态/权限）
     *  3. 领域操作（+ Outbox 写事件）
     *  4. 记录指标
     */
    @Transactional
    public ResourceResult create(ResourceCommand command) {
        var start = java.time.Instant.now();

        // 1. 幂等检查
        String idempotencyKey = "resource:create:" + command.requestId();
        if (!idempotencyService.tryAcquire(idempotencyKey, java.time.Duration.ofMinutes(10))) {
            log.info("Idempotent hit: requestId={}", command.requestId());
            metrics.recordIdempotencyHit();
            return domainService.findByRequestId(command.requestId());
        }

        // 2. 业务校验
        // validateBusinessRules(command);

        // 3. 领域操作
        ResourceResult result = domainService.create(command);

        // 4. 指标
        metrics.recordSuccess(java.time.Duration.between(start, java.time.Instant.now()));

        log.info("Resource created: id={}, requestId={}", result.id(), command.requestId());
        return result;
    }
}
