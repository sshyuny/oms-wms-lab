package com.sshyuny.omswms.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductCreateRequest(
        @NotBlank String skuCode,
        @NotBlank String name,
        @NotNull @Positive Long price
) {
}
