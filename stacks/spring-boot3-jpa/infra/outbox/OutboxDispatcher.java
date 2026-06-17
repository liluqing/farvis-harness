package com.example.infra.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Outbox 事件投递器。
 *
 * 定时扫描 outbox_events 表中状态为 NEW 或 RETRY 的事件，
 * 投递到消息队列。投递成功标记 SENT，失败标记 RETRY/DEAD。
 *
 * 约定：
 *   - 投递间隔：1s（可配置）
 *   - 每次最多取 100 条
 *   - 重试 ≥ 10 次后标记 DEAD
 *   - DEAD 事件触发告警
 *
 * 使用方式：
 *   替换 sendToMessageQueue() 方法体为实际的 Kafka/RabbitMQ 发送逻辑。
 */
@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private static final int MAX_RETRY = 10;
    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository eventRepository;
    // private final KafkaTemplate<String, String> kafkaTemplate;  // 按需替换

    public OutboxDispatcher(OutboxEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void dispatch() {
        List<OutboxEvent> events = eventRepository
            .findTop100ByStatusInOrderByIdAsc(
                List.of(OutboxEvent.Status.NEW, OutboxEvent.Status.RETRY)
            );

        if (events.isEmpty()) return;

        log.debug("Dispatching {} outbox events", events.size());

        for (OutboxEvent event : events) {
            try {
                sendToMessageQueue(event);
                event.markSent();
                eventRepository.save(event);
                log.info("Outbox event sent: type={}, aggregateId={}",
                    event.getEventType(), event.getAggregateId());
            } catch (Exception ex) {
                if (event.getRetryCount() >= MAX_RETRY) {
                    event.markDead(ex.getMessage());
                    log.error("Outbox event DEAD: type={}, aggregateId={}, error={}",
                        event.getEventType(), event.getAggregateId(), ex.getMessage());
                } else {
                    event.markRetry(ex.getMessage());
                    log.warn("Outbox event retry #{}/{}: type={}, aggregateId={}",
                        event.getRetryCount(), MAX_RETRY,
                        event.getEventType(), event.getAggregateId());
                }
                eventRepository.save(event);
            }
        }
    }

    /**
     * 实际的消息发送逻辑。需要替换为 Kafka/RabbitMQ 的发送代码。
     */
    private void sendToMessageQueue(OutboxEvent event) {
        // TODO: 替换为实际的 Kafka/RabbitMQ 发送
        // kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload()).get(10, TimeUnit.SECONDS);
        throw new UnsupportedOperationException("Replace with actual message queue sending logic");
    }

    /**
     * 积压监控：暴露给 Metrics。
     */
    public long countPending() {
        return eventRepository.countByStatusIn(
            List.of(OutboxEvent.Status.NEW, OutboxEvent.Status.RETRY)
        );
    }
}
