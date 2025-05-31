package com.edx.spring.config.spring_config_central;

import com.edx.spring.config.central.SpringConfigCentralApplication;
import org.springframework.boot.SpringApplication;

public class TestSpringConfigCentralApplication {

	public static void main(String[] args) {
		SpringApplication.from(SpringConfigCentralApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
