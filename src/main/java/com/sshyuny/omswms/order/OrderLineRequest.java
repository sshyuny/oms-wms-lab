package com.sshyuny.omswms.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderLineRequest(
        @NotNull Long productId,
        @NotNull @Positive Integer quantity
) {
}
