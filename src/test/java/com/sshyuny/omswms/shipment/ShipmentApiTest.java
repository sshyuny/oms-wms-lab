package com.sshyuny.omswms.shipment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sshyuny.omswms.IntegrationTest;
import org.junit.jupiter.api.Test;

class ShipmentApiTest extends IntegrationTest {

    @Test
    void 피킹_패킹_출고를_거치면_주문이_출고완료된다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);
        Long orderId = createOrder(productId, 1);
        pay(orderId);
        Long shipmentId = shipmentIdOf(orderId);

        mockMvc.perform(post("/api/shipments/{id}/picking", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKING"));
        mockMvc.perform(post("/api/shipments/{id}/packing", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PACKED"));
        mockMvc.perform(post("/api/shipments/{id}/shipping", shipmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.shippedAt").isNotEmpty());

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }
}
