/*
 * User: eldad
 * Date: 9/1/2025
 *
 * Copyright (2005) IDI. All rights reserved.
 * This software is a proprietary information of Israeli Direct Insurance.
 * Created by IntelliJ IDEA.
 */
package com.edx.spring.config.central.server.service;

/**
 *
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
public class ConfigResponseInterceptor implements HandlerInterceptor {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		log.debug("Intercepting config request: {}", request.getRequestURI());
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
	                            Object handler, Exception ex) {
		if (isConfigEndpoint(request) && response.getStatus() == 200) {
			log.info("Config response intercepted for: {}", request.getRequestURI());
			// Custom logic can be added here if needed
		}
	}

	private boolean isConfigEndpoint(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri != null && (uri.contains("/config/") || uri.matches(".*/[^/]+/[^/]+(/[^/]+)?$"));
	}
}
