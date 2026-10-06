package com.sshyuny.omswms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SchemaTest extends IntegrationTest {

    @Test
    void 기본_창고가_등록되어_있다() {
        String code = jdbcTemplate.queryForObject("select code from warehouse where id = 1", String.class);

        assertThat(code).isEqualTo("WH-01");
    }
}
