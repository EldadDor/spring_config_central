package com.edx.spring.config.central.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.config.CompositeConfiguration;
import org.springframework.cloud.config.server.config.EnvironmentRepositoryConfiguration;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;

@SpringBootApplication(
//		exclude = {CompositeConfiguration.class,
//				EnvironmentRepositoryConfiguration.class}
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
