package com.sshyuny.omswms.order;

import java.time.LocalDateTime;

public class OrderFixture {

    private final OrderRepository orderRepository;

    public OrderFixture(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order persist(Long productId, Integer quantity, LocalDateTime orderedAt) {
        Order order = new Order(1L, orderedAt);
        order.addLine(new OrderLine(productId, "테스트 상품", 10_000L,  quantity));
        order.setTotalProductAmount(10_000L);
        order.setShippingFee(3_000L);
        order.setCouponDiscountAmount(0L);
        order.setPaymentAmount(13_000L);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        return orderRepository.save(order);
    }
}
