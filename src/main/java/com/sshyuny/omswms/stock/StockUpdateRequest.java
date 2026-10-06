package com.sshyuny.omswms.stock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StockUpdateRequest(@NotNull @PositiveOrZero Integer quantity) {
}
