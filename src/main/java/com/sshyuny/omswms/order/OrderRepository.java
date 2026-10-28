package com.sshyuny.omswms.order;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        select distinct o
        from Order o
        join o.lines ol
        where ol.productId = :productId
        and o.orderedAt < :invalidTime
        and o.status = :status
        """)
    List<Order> findInvalidOrder(@Param("productId") Long productId, @Param("invalidTime") LocalDateTime invalidTime, @Param("status") OrderStatus status);

}
