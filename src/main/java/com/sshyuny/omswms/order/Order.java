package com.sshyuny.omswms.order;

import com.sshyuny.omswms.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private Long totalProductAmount;

    @Column(nullable = false)
    private Long shippingFee;

    @Column(length = 50)
    private String couponCode;

    @Column(nullable = false)
    private Long couponDiscountAmount;

    @Column(nullable = false)
    private Long paymentAmount;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    public Order(Long customerId, LocalDateTime orderedAt) {
        this.customerId = customerId;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.orderedAt = orderedAt;
    }

    public void addLine(OrderLine line) {
        lines.add(line);
        line.setOrder(this);
    }
}
