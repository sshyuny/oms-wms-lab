package com.sshyuny.omswms.shipment;

public class ShipmentFixture {

    private final ShipmentRepository shipmentRepository;

    private final Long warehouseId = 1L;

    public ShipmentFixture(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    public Shipment persist(Long orderId) {
        Shipment shipment = new Shipment();
        shipment.setOrderId(orderId);
        shipment.setWarehouseId(warehouseId);
        shipment.setStatus(ShipmentStatus.READY);
        return shipmentRepository.save(shipment);
    }

    
}
