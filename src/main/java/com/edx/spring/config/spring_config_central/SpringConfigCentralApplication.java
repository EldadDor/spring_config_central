package com.edx.spring.config.spring_config_central;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class SpringConfigCentralApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringConfigCentralApplication.class, args);
	}

}
