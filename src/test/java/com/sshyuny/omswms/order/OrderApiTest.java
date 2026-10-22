package com.sshyuny.omswms.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.jayway.jsonpath.JsonPath;
import com.sshyuny.omswms.IntegrationTest;
import com.sshyuny.omswms.common.ErrorCode;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class OrderApiTest extends IntegrationTest {

    @Test
    void 동시에_주문이_몰릴때_재고가_음수가_되지_않는다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        boolean completed;
        try {
            CountDownLatch ready = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        ready.await();
                        order(productId);
                    } catch (Exception e) {
                        // 재고 부족 코드(OUT_OF_STOCK) 이외의 예외만 여기에 오지만 정확히 재고가 0인지 검증하고 있으므로 별도 처리하지 않아도 된다.
                    } finally {
                        done.countDown();
                    }
                });
            }
            ready.countDown();
            completed = done.await(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }

        assertThat(completed).isTrue();
        String res = mockMvc.perform(get("/api/stocks/{productId}", productId))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        Long quantity = JsonPath.parse(res).read("$.quantity", Long.class);
        assertThat(quantity).isEqualTo(0);
    }

    void order(Long productId) throws Exception {
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "customerId": 1,
                          "lines": [
                            {"productId": %d, "quantity": 1}
                          ]
                        }
                        """.formatted(productId)));
    }

    @Test
    void 주문할때_일반적인_상황에서는_음수_재고가_발생하지_않는다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);
        createOrder(productId, 8);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": 1,
                                  "lines": [
                                    {"productId": %d, "quantity": 4}
                                  ]
                                }
                                """.formatted(productId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.OUT_OF_STOCK.name()));

        mockMvc.perform(get("/api/stocks/{productId}", productId))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void 주문하면_결제대기_상태가_되고_재고가_차감된다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);
        setStock(productId, 10);

        Long orderId = createOrder(productId, 2);

        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.totalProductAmount").value(20_000))
                .andExpect(jsonPath("$.shippingFee").value(3_000))
                .andExpect(jsonPath("$.couponDiscountAmount").value(0))
                .andExpect(jsonPath("$.paymentAmount").value(23_000))
                .andExpect(jsonPath("$.lines.length()").value(1))
                .andExpect(jsonPath("$.lines[0].productId").value(productId))
                .andExpect(jsonPath("$.lines[0].unitPrice").value(10_000))
                .andExpect(jsonPath("$.lines[0].quantity").value(2))
                .andExpect(jsonPath("$.lines[0].lineAmount").value(20_000));

        mockMvc.perform(get("/api/stocks/{productId}", productId))
                .andExpect(jsonPath("$.quantity").value(8));
    }

    @Test
    void 상품금액이_5만원_이상이면_배송비가_무료이고_쿠폰_할인이_적용된다() throws Exception {
        Long shirtId = createProduct("SKU-SHIRT", 30_000L);
        Long pantsId = createProduct("SKU-PANTS", 25_000L);
        setStock(shirtId, 10);
        setStock(pantsId, 10);
        postJson("/api/coupons", """
                {"code": "WELCOME5000", "discountAmount": 5000, "minOrderAmount": 50000}
                """, status().isCreated());

        postJson("/api/orders", """
                {
                  "customerId": 1,
                  "lines": [
                    {"productId": %d, "quantity": 1},
                    {"productId": %d, "quantity": 1}
                  ],
                  "couponCode": "WELCOME5000"
                }
                """.formatted(shirtId, pantsId), status().isCreated());

        mockMvc.perform(get("/api/orders/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductAmount").value(55_000))
                .andExpect(jsonPath("$.shippingFee").value(0))
                .andExpect(jsonPath("$.couponCode").value("WELCOME5000"))
                .andExpect(jsonPath("$.couponDiscountAmount").value(5_000))
                .andExpect(jsonPath("$.paymentAmount").value(50_000))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }
}
