package com.sshyuny.omswms.shipment;

import java.time.LocalDateTime;

public record ShipmentResponse(
        Long id,
        Long orderId,
        Long warehouseId,
        ShipmentStatus status,
        LocalDateTime shippedAt
) {

    public static ShipmentResponse from(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getWarehouseId(),
                shipment.getStatus(),
                shipment.getShippedAt());
    }
}
