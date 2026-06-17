package com.example.infra.outbox;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Outbox 事件实体 —— 确保"DB 事务 + 消息发送"的原子性。
 *
 * 业务代码在事务中同时写入业务数据和 OutboxEvent，
 * 独立投递器异步发送消息，发送成功后标记为 SENT。
 *
 * 使用方式：
 *  1. 业务 Service 中在 @Transactional 内同时写业务实体和 OutboxEvent
 *  2. OutboxDispatcher（@Scheduled）异步投递
 *  3. 投递成功 → markSent()，失败 → markRetry()
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 事件类型，如 "order_created", "video_generated" */
    @Column(nullable = false, length = 100)
    private String eventType;

    /** 聚合根 ID */
    @Column(nullable = false, length = 100)
    private String aggregateId;

    /** 事件 payload（JSON） */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** 状态：NEW / SENT / RETRY / DEAD */
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.NEW;

    /** 已重试次数 */
    @Column(nullable = false)
    private int retryCount = 0;

    /** 最后一次错误信息 */
    @Column(length = 2000)
    private String lastError;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public enum Status { NEW, SENT, RETRY, DEAD }

    // ====== 工厂方法 ======

    public static OutboxEvent create(String eventType, String aggregateId, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.eventType = eventType;
        event.aggregateId = aggregateId;
        event.payload = payload;
        event.status = Status.NEW;
        event.createdAt = Instant.now();
        event.updatedAt = Instant.now();
        return event;
    }

    // ====== 状态变更 ======

    public void markSent() {
        this.status = Status.SENT;
        this.updatedAt = Instant.now();
    }

    public void markRetry(String error) {
        this.status = Status.RETRY;
        this.retryCount++;
        this.lastError = error;
        this.updatedAt = Instant.now();
    }

    public void markDead(String error) {
        this.status = Status.DEAD;
        this.lastError = error;
        this.updatedAt = Instant.now();
    }

    // ====== Getters ======

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public String getAggregateId() { return aggregateId; }
    public String getPayload() { return payload; }
    public Status getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
