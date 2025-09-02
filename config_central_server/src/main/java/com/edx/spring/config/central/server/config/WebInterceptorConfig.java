/*
 * User: eldad
 * Date: 9/1/2025
 *
 * Copyright (2005) IDI. All rights reserved.
 * This software is a proprietary information of Israeli Direct Insurance.
 * Created by IntelliJ IDEA.
 */
package com.edx.spring.config.central.server.config;

import com.edx.spring.config.central.server.service.ConfigResponseInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebInterceptorConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new ConfigResponseInterceptor())
				.addPathPatterns("/**")
				.excludePathPatterns("/actuator/**", "/error");
	}
}