package com.sshyuny.omswms.warehouse;

import jakarta.validation.constraints.NotNull;

public record WarehouseStatusChangeRequest(@NotNull WarehouseStatus status) {
}
