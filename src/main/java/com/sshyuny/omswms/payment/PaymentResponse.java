package com.sshyuny.omswms.payment;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long orderId,
        String pgTransactionId,
        Long amount,
        PaymentStatus status,
        LocalDateTime approvedAt,
        LocalDateTime cancelledAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getPgTransactionId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getApprovedAt(),
                payment.getCancelledAt());
    }
}
