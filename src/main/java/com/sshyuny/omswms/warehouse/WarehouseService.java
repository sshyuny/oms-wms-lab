package com.sshyuny.omswms.warehouse;

import com.sshyuny.omswms.common.BusinessException;
import com.sshyuny.omswms.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Transactional
    public WarehouseResponse changeStatus(Long id, WarehouseStatusChangeRequest request) {
        Warehouse warehouse = getWarehouseEntity(id);
        warehouse.setStatus(request.status());
        return WarehouseResponse.from(warehouse);
    }

    public WarehouseResponse getWarehouse(Long id) {
        return WarehouseResponse.from(getWarehouseEntity(id));
    }

    private Warehouse getWarehouseEntity(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND));
    }
}
