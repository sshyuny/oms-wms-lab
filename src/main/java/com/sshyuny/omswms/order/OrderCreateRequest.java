package com.sshyuny.omswms.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderCreateRequest(
        @NotNull Long customerId,
        @NotEmpty @Valid List<OrderLineRequest> lines,
        String couponCode
) {
}
