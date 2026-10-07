package com.sshyuny.omswms.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    DUPLICATE_SKU_CODE(HttpStatus.CONFLICT, "이미 등록된 SKU 코드입니다."),
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "재고 정보를 찾을 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "재고가 부족합니다."),
    WAREHOUSE_NOT_FOUND(HttpStatus.NOT_FOUND, "창고를 찾을 수 없습니다."),
    WAREHOUSE_SUSPENDED(HttpStatus.CONFLICT, "출고가 중지된 창고입니다."),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    DUPLICATE_COUPON_CODE(HttpStatus.CONFLICT, "이미 등록된 쿠폰 코드입니다."),
    COUPON_MIN_ORDER_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "쿠폰 최소 주문 금액을 충족하지 않습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.CONFLICT, "현재 주문 상태에서 처리할 수 없습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    SHIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "출고 정보를 찾을 수 없습니다."),
    INVALID_SHIPMENT_STATUS(HttpStatus.CONFLICT, "현재 출고 상태에서 처리할 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
