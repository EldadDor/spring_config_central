package com.edx.spring.config.central.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(
		/*exclude = {
				org.springframework.cloud.config.server.config.EnvironmentRepositoryConfiguration.class
		}*/
)
@ComponentScan({"com.edx.spring.config", "com.edx.spring.config.central"})
@EnableConfigServer
public class SpringConfigCentralApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringConfigCentralApplication.class, args);
	}
	
}
