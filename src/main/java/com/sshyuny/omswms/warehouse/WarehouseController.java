package com.sshyuny.omswms.warehouse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/{id}")
    public WarehouseResponse getWarehouse(@PathVariable Long id) {
        return warehouseService.getWarehouse(id);
    }

    @PatchMapping("/{id}/status")
    public WarehouseResponse changeStatus(@PathVariable Long id,
                                          @Valid @RequestBody WarehouseStatusChangeRequest request) {
        return warehouseService.changeStatus(id, request);
    }
}
