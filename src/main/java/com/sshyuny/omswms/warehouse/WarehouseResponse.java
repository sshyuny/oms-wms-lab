package com.sshyuny.omswms.warehouse;

public record WarehouseResponse(Long id, String code, String name, WarehouseStatus status) {

    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(warehouse.getId(), warehouse.getCode(), warehouse.getName(), warehouse.getStatus());
    }
}
