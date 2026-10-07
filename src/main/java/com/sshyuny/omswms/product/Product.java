package com.sshyuny.omswms.product;

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
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String skuCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private Long price;

    public Product(String skuCode, String name, Long price) {
        this.skuCode = skuCode;
        this.name = name;
        this.price = price;
    }
}
