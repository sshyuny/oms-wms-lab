package com.sshyuny.omswms.common;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByEventTypeAndStatus(String eventType, String status);

}
