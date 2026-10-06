package com.sshyuny.omswms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

    private static final List<String> TABLES = List.of(
            "product", "stock", "coupon", "orders", "order_line", "payment", "shipment");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUpDatabase() {
        TABLES.forEach(table -> jdbcTemplate.execute("truncate table " + table));
        jdbcTemplate.update("update warehouse set status = 'ACTIVE'");
    }

    protected String postJson(String url, String body, ResultMatcher expectedStatus) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(expectedStatus)
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    protected Long createProduct(String skuCode, long price) throws Exception {
        String response = postJson("/api/products", """
                {"skuCode": "%s", "name": "테스트 상품 %s", "price": %d}
                """.formatted(skuCode, skuCode, price), status().isCreated());
        return JsonPath.parse(response).read("$.id", Long.class);
    }

    protected void setStock(Long productId, int quantity) throws Exception {
        mockMvc.perform(put("/api/stocks/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": %d}
                                """.formatted(quantity)))
                .andExpect(status().isOk());
    }

    protected Long createOrder(Long productId, int quantity) throws Exception {
        String response = postJson("/api/orders", """
                {"customerId": 1, "lines": [{"productId": %d, "quantity": %d}]}
                """.formatted(productId, quantity), status().isCreated());
        return JsonPath.parse(response).read("$.id", Long.class);
    }

    protected String pay(Long orderId) throws Exception {
        String response = postJson("/api/orders/" + orderId + "/payment", "", status().isCreated());
        return JsonPath.parse(response).read("$.pgTransactionId", String.class);
    }
}
