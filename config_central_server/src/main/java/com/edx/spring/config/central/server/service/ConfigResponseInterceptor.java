package com.edx.spring.config.central.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class ConfigResponseInterceptor implements HandlerInterceptor {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
	                            Object handler, Exception ex) throws Exception {

		if (isNexlConfigEndpoint(request) && response.getStatus() == 200) {
			log.info("Config response intercepted for NEXL: {}", request.getRequestURI());

			ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
			byte[] content = wrapper.getContentAsByteArray();

			if (content.length > 0) {
				String originalResponse = new String(content, StandardCharsets.UTF_8);
				String modifiedResponse = extractNexlData(originalResponse);

				// Reset and write modified response
				wrapper.resetBuffer();
				wrapper.setContentType("application/json");
				wrapper.getOutputStream().write(modifiedResponse.getBytes(StandardCharsets.UTF_8));
				wrapper.copyBodyToResponse();
			}
		}
	}

	private boolean isNexlConfigEndpoint(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri != null && uri.contains("/nexl") && uri.matches(".*/[^/]+/[^/]+(/[^/]+)?$");
	}

	private String extractNexlData(String originalResponse) {
		try {
			Environment environment = objectMapper.readValue(originalResponse, Environment.class);

			if (!environment.getPropertySources().isEmpty()) {
				PropertySource propertySource = environment.getPropertySources().get(0);
				Object source = propertySource.getSource();

				// Convert to Map<String, Object> safely
				if (source instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> sourceMap = (Map<String, Object>) source;
					return objectMapper.writeValueAsString(sourceMap);
				}
			}
		} catch (Exception e) {
			log.error("Failed to extract NEXL data from response", e);
		}

		return originalResponse;
	}
}
