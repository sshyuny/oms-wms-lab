package com.sshyuny.omswms.coupon;

public record CouponResponse(Long id, String code, Long discountAmount, Long minOrderAmount) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(coupon.getId(), coupon.getCode(), coupon.getDiscountAmount(), coupon.getMinOrderAmount());
    }
}
