/*
 * User: eldad
 * Date: 9/5/2025
 *
 * Copyright (2005) IDI. All rights reserved.
 * This software is a proprietary information of Israeli Direct Insurance.
 * Created by IntelliJ IDEA.
 */
package com.edx.spring.config.central.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Bean
	@Order(1)
	public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatchers(matchers -> matchers
						.requestMatchers(
								new AntPathRequestMatcher("/swagger-ui/**"),
								new AntPathRequestMatcher("/swagger-ui.html"),
								new AntPathRequestMatcher("/v3/api-docs/**"),
								new AntPathRequestMatcher("/swagger-resources/**"),
								new AntPathRequestMatcher("/webjars/**"),
								new AntPathRequestMatcher("/admin/**")
						)
				)
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
				.csrf(csrf -> csrf.disable());

		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
				.csrf(csrf -> csrf.disable());

		return http.build();
	}
}
