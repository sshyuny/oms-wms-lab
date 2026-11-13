package com.sshyuny.omswms.shipment;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sshyuny.omswms.common.OutboxEvent;
import com.sshyuny.omswms.common.OutboxEventJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShipmentScheduler {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ShipmentService shipmentService;

    private final ObjectMapper mapper;

    @Scheduled(fixedRate = 10000)
    public void createShipment() {

        List<OutboxEvent> shipmentCreationEvents = outboxEventJpaRepository.findByEventTypeAndStatus(
            "SHIPMENT_CREATION_REQUESTED", "PENDING");
        
        for (OutboxEvent event : shipmentCreationEvents) {
            String payload = event.getPayload();
            ShipmentCreation shipmentCreation = mapper.readValue(payload, ShipmentCreation.class);
            Long orderId = Long.valueOf(shipmentCreation.orderId());

            try {
                shipmentService.createShipment(orderId);
                event.setStatus("PROCESSED");
                event.setProcessedAt(LocalDateTime.now());
                outboxEventJpaRepository.save(event);
            } catch (Exception e) {
                log.error("출고 생성 이벤트 처리 실패: eventId={}, orderId={}", event.getId(), orderId, e);
            }
        }
    }

    record ShipmentCreation(String orderId) {}
    
}
