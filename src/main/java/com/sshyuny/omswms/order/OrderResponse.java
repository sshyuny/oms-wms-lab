package com.sshyuny.omswms.order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        OrderStatus status,
        Long totalProductAmount,
        Long shippingFee,
        String couponCode,
        Long couponDiscountAmount,
        Long paymentAmount,
        LocalDateTime orderedAt,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt,
        List<OrderLineResponse> lines
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalProductAmount(),
                order.getShippingFee(),
                order.getCouponCode(),
                order.getCouponDiscountAmount(),
                order.getPaymentAmount(),
                order.getOrderedAt(),
                order.getPaidAt(),
                order.getCancelledAt(),
                order.getLines().stream().map(OrderLineResponse::from).toList());
    }
}
