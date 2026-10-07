package com.sshyuny.omswms.coupon;

import com.sshyuny.omswms.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private Long discountAmount;

    @Column(nullable = false)
    private Long minOrderAmount;

    public Coupon(String code, Long discountAmount, Long minOrderAmount) {
        this.code = code;
        this.discountAmount = discountAmount;
        this.minOrderAmount = minOrderAmount;
    }
}
