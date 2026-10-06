package com.sshyuny.omswms.stock;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductId(Long productId);

    @Modifying(flushAutomatically = true)
    @Query("update Stock s set s.quantity = s.quantity - :quantity where s.productId = :productId")
    int decreaseQuantity(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying(flushAutomatically = true)
    @Query("update Stock s set s.quantity = s.quantity + :quantity where s.productId = :productId")
    int increaseQuantity(@Param("productId") Long productId, @Param("quantity") int quantity);
}
