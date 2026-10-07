package com.sshyuny.omswms.payment;

import com.sshyuny.omswms.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false, length = 64)
    private String pgTransactionId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false)
    private LocalDateTime approvedAt;

    private LocalDateTime cancelledAt;

    public Payment(Long orderId, String pgTransactionId, Long amount, LocalDateTime approvedAt) {
        this.orderId = orderId;
        this.pgTransactionId = pgTransactionId;
        this.amount = amount;
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = approvedAt;
    }
}
