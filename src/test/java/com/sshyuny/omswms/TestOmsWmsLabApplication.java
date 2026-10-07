package com.sshyuny.omswms;

import org.springframework.boot.SpringApplication;

public class TestOmsWmsLabApplication {

	public static void main(String[] args) {
		SpringApplication.from(OmsWmsLabApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
