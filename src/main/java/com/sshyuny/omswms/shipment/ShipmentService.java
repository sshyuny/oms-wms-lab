package com.sshyuny.omswms.shipment;

import com.sshyuny.omswms.common.BusinessException;
import com.sshyuny.omswms.common.ErrorCode;
import com.sshyuny.omswms.warehouse.Warehouse;
import com.sshyuny.omswms.warehouse.WarehouseRepository;
import com.sshyuny.omswms.warehouse.WarehouseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final WarehouseRepository warehouseRepository;

    @Value("${app.default-warehouse-id}")
    private Long defaultWarehouseId;

    @Transactional
    public void createShipment(Long orderId) {
        Warehouse warehouse = warehouseRepository.findById(defaultWarehouseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND));
        if (warehouse.getStatus() == WarehouseStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.WAREHOUSE_SUSPENDED);
        }
        shipmentRepository.save(new Shipment(orderId, warehouse.getId()));
    }

    public ShipmentResponse getShipment(Long id) {
        return ShipmentResponse.from(getShipmentEntity(id));
    }

    public ShipmentResponse getShipmentByOrderId(Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPMENT_NOT_FOUND));
        return ShipmentResponse.from(shipment);
    }

    private Shipment getShipmentEntity(Long id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHIPMENT_NOT_FOUND));
    }
}
