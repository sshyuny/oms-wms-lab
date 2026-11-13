package com.sshyuny.omswms.shipment;

import java.util.Map;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sshyuny.omswms.common.OutboxEvent;
import com.sshyuny.omswms.common.OutboxEventJpaRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Repository
@RequiredArgsConstructor
public class ShipmentEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    @Transactional
    public void publishShipmentCreation(Long orderId) {
        String payload = mapper.writeValueAsString(Map.of("orderId", orderId));

        OutboxEvent createShipmentOutbox = new OutboxEvent("SHIPMENT_CREATION_REQUESTED", payload);
        outboxEventJpaRepository.save(createShipmentOutbox);
    }
    
}
