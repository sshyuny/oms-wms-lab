package com.sshyuny.omswms.stock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sshyuny.omswms.IntegrationTest;
import org.junit.jupiter.api.Test;

class StockApiTest extends IntegrationTest {

    @Test
    void 재고_수량을_변경하고_조회한다() throws Exception {
        Long productId = createProduct("SKU-001", 10_000L);

        setStock(productId, 10);

        mockMvc.perform(get("/api/stocks/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.soldOut").value(false));
    }
}
