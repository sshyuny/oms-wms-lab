package com.sshyuny.omswms.coupon;

import com.sshyuny.omswms.common.BusinessException;
import com.sshyuny.omswms.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {
        if (couponRepository.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.DUPLICATE_COUPON_CODE);
        }
        Coupon coupon = couponRepository.save(
                new Coupon(request.code(), request.discountAmount(), request.minOrderAmount()));
        return CouponResponse.from(coupon);
    }
}
