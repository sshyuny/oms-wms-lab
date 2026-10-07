package com.sshyuny.omswms.payment;

import com.sshyuny.omswms.common.BusinessException;
import com.sshyuny.omswms.common.ErrorCode;
import com.sshyuny.omswms.order.Order;
import com.sshyuny.omswms.order.OrderRepository;
import com.sshyuny.omswms.order.OrderStatus;
import com.sshyuny.omswms.shipment.ShipmentService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ShipmentService shipmentService;
    private final FakePgClient fakePgClient;

    @Transactional
    public PaymentResponse approve(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        LocalDateTime now = LocalDateTime.now();
        String pgTransactionId = fakePgClient.approve(order.getPaymentAmount());
        Payment payment = paymentRepository.save(
                new Payment(order.getId(), pgTransactionId, order.getPaymentAmount(), now));

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(now);
        shipmentService.createShipment(order.getId());

        return PaymentResponse.from(payment);
    }

    @Transactional
    public void cancel(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
        fakePgClient.cancel(payment.getPgTransactionId());
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setCancelledAt(LocalDateTime.now());
    }
}
