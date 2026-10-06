package com.sshyuny.omswms.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sshyuny.omswms.IntegrationTest;
import org.junit.jupiter.api.Test;

class CouponApiTest extends IntegrationTest {

    @Test
    void 쿠폰을_등록한다() throws Exception {
        String response = postJson("/api/coupons", """
                {"code": "WELCOME5000", "discountAmount": 5000, "minOrderAmount": 30000}
                """, status().isCreated());

        DocumentContext json = JsonPath.parse(response);
        assertThat(json.read("$.code", String.class)).isEqualTo("WELCOME5000");
        assertThat(json.read("$.discountAmount", Long.class)).isEqualTo(5_000L);
        assertThat(json.read("$.minOrderAmount", Long.class)).isEqualTo(30_000L);
    }
}
