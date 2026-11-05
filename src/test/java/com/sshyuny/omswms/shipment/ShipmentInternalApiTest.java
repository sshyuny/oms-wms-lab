package com.sshyuny.omswms.shipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sshyuny.omswms.IntegrationTest;
import com.sshyuny.omswms.order.Order;
import com.sshyuny.omswms.order.OrderFixture;
import com.sshyuny.omswms.order.OrderRepository;
import com.sshyuny.omswms.order.OrderStatus;

public class ShipmentInternalApiTest extends IntegrationTest {

    @Autowired
    OrderRepository orderRepository;
    @Autowired 
    ShipmentService shipmentService;
    @Autowired
    ShipmentRepository shipmentRepository;

    private OrderFixture orderFixture;
    private ShipmentFixture shipmentFixture;

    @BeforeEach 
    void setUp() {
        orderFixture = new OrderFixture(orderRepository);
        shipmentFixture = new ShipmentFixture(shipmentRepository);
    }

    @Test
    void 결제됐지만_출고가_완료되지않은_주문을_일괄_출고한다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        LocalDateTime now = LocalDateTime.now();
        
        Order order1 = orderFixture.persist(productId, 10, OrderStatus.PAID, now);
        Order order2 = orderFixture.persist(productId, 8, OrderStatus.PAID, now);

        mockMvc.perform(post("/internal/shipments/create"))
            .andExpect(status().isOk());

        ShipmentResponse shipment1 = shipmentService.getShipmentByOrderId(order1.getId());
        ShipmentResponse shipment2 = shipmentService.getShipmentByOrderId(order2.getId());

        assertThat(shipment1).isNotNull();
        assertThat(shipment2).isNotNull();
    }

    @Test
    void 출고가_완료된_주문은_중복_출고되지_않는다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        LocalDateTime now = LocalDateTime.now();
        
        Order order1 = orderFixture.persist(productId, 10, OrderStatus.PAID, now);
        Order order2 = orderFixture.persist(productId, 8, OrderStatus.PAID, now);
        Shipment shipment1Before = shipmentFixture.persist(order1.getId());
        Shipment shipment2Before = shipmentFixture.persist(order2.getId());

        mockMvc.perform(post("/internal/shipments/create"))
            .andExpect(status().isOk());

        ShipmentResponse shipment1 = shipmentService.getShipmentByOrderId(order1.getId());
        ShipmentResponse shipment2 = shipmentService.getShipmentByOrderId(order2.getId());

        assertThat(shipment1Before.getId()).isEqualTo(shipment1.id());
        assertThat(shipment2Before.getId()).isEqualTo(shipment2.id());
    }
    
}
