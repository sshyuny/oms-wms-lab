package com.sshyuny.omswms.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sshyuny.omswms.IntegrationTest;
import org.junit.jupiter.api.Test;

class ProductApiTest extends IntegrationTest {

    @Test
    void 상품을_등록하고_조회한다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId))
                .andExpect(jsonPath("$.skuCode").value("SKU-001"))
                .andExpect(jsonPath("$.price").value(10_000));
    }

    @Test
    void 상품을_등록하면_재고_0개가_함께_생성된다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);

        mockMvc.perform(get("/api/stocks/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.soldOut").value(true));
    }
}
