package com.sshyuny.omswms.payment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.sshyuny.omswms.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

class PaymentShipmentFailureApiTest extends IntegrationTest {

    @Autowired
    private FakePgClient fakePgClient;

    @Test 
    void 출고지시가_실패해도_결제는_성공한다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);
        Long orderId = createOrder(productId, 2);

        jdbcTemplate.update("update warehouse set status = 'SUSPENDED'");

        String response = postJson("/api/orders/" + orderId + "/payment", "", status().isCreated());

        DocumentContext payment = JsonPath.parse(response);
        assertThat(payment.read("$.orderId", Long.class)).isEqualTo(orderId);
        assertThat(payment.read("$.amount", Long.class)).isEqualTo(23_000L);
        assertThat(payment.read("$.status", String.class)).isEqualTo("APPROVED");

        String pgTransactionId = payment.read("$.pgTransactionId", String.class);
        assertThat(fakePgClient.findTransaction(pgTransactionId))
                .hasValueSatisfying(transaction -> {
                    assertThat(transaction.amount()).isEqualTo(23_000L);
                    assertThat(transaction.status()).isEqualTo(PgTransactionStatus.APPROVED);
                });

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());
    }
}
