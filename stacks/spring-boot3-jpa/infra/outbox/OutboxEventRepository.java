package com.example.infra.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop100ByStatusInOrderByIdAsc(List<OutboxEvent.Status> statuses);

    long countByStatusIn(List<OutboxEvent.Status> statuses);
}
