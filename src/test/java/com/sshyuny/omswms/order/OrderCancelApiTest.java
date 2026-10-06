package com.sshyuny.omswms.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sshyuny.omswms.IntegrationTest;
import com.sshyuny.omswms.payment.FakePgClient;
import com.sshyuny.omswms.payment.PgTransaction;
import com.sshyuny.omswms.payment.PgTransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderCancelApiTest extends IntegrationTest {

    @Autowired
    private FakePgClient fakePgClient;

    @Test
    void 결제_전_주문을_취소하면_재고가_복구된다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);
        Long orderId = createOrder(productId, 3);

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());

        mockMvc.perform(get("/api/stocks/{productId}", productId))
                .andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    void 출고_준비_중인_주문을_취소하면_결제와_출고지시도_취소된다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);
        Long orderId = createOrder(productId, 3);
        String pgTransactionId = pay(orderId);

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/shipments").param("orderId", String.valueOf(orderId)))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(get("/api/stocks/{productId}", productId))
                .andExpect(jsonPath("$.quantity").value(10));
        assertThat(fakePgClient.findTransaction(pgTransactionId))
                .map(PgTransaction::status)
                .hasValue(PgTransactionStatus.CANCELLED);
    }
}
