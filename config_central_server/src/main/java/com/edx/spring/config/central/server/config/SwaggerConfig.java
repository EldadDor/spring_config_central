/*
 * User: eldad
 * Date: 9/5/2025
 *
 * Copyright (2005) IDI. All rights reserved.
 * This software is a proprietary information of Israeli Direct Insurance.
 * Created by IntelliJ IDEA.
 */
package com.edx.spring.config.central.server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 *
 */

@Configuration
public class SwaggerConfig {

	@Value("${server.port:8888}")
	private int serverPort;

	@Bean
	public OpenAPI configServerOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Spring Config Server API")
						.description("REST API for Spring Cloud Config Server with custom providers")
						.version("1.0.0"))
				.servers(List.of(
						new Server()
								.url("http://localhost:" + serverPort)
								.description("Local server")
				));
	}
}
