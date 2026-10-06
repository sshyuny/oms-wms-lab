package com.sshyuny.omswms.warehouse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sshyuny.omswms.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class WarehouseApiTest extends IntegrationTest {

    @Test
    void 창고_출고를_중지한다() throws Exception {
        mockMvc.perform(patch("/api/warehouses/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "SUSPENDED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(get("/api/warehouses/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("WH-01"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }
}
