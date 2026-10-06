package com.sshyuny.omswms.coupon;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CouponCreateRequest(
        @NotBlank String code,
        @NotNull @Positive Long discountAmount,
        @NotNull @PositiveOrZero Long minOrderAmount
) {
}
