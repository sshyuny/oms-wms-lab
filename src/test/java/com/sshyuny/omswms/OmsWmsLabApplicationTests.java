package com.sshyuny.omswms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OmsWmsLabApplicationTests {

	@Test
	void contextLoads() {
	}

}
