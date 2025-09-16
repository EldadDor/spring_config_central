package com.edx.spring.config.central.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class ConfigResponseInterceptor implements HandlerInterceptor {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ApplicationContext context;

	public ConfigResponseInterceptor(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		log.info("Config request intercepted: {}", request.getRequestURI());
		return HandlerInterceptor.super.preHandle(request, response, handler);
	}
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
	                            Object handler, Exception ex) throws Exception {

		if (isNexlConfigEndpoint(request) && response.getStatus() == 200) {
			log.info("Config response intercepted for NEXL: {}", request.getRequestURI());

			// Try to find ContentCachingResponseWrapper in the wrapper chain
			ContentCachingResponseWrapper cachingWrapper = findContentCachingWrapper(response);

			if (cachingWrapper != null) {
				byte[] content = cachingWrapper.getContentAsByteArray();

				if (content.length > 0) {
					String originalResponse = new String(content, StandardCharsets.UTF_8);
					String modifiedResponse = extractNexlData(originalResponse);

					// Clear the existing content and write the modified response
					cachingWrapper.resetBuffer();
					cachingWrapper.setContentType("application/json");
					cachingWrapper.getOutputStream().write(modifiedResponse.getBytes(StandardCharsets.UTF_8));
					cachingWrapper.copyBodyToResponse();
				}
			} else {
				// Alternative approach: Try to modify response directly
				handleNonCachingResponse(response, request);
			}
		}
	}

	private ContentCachingResponseWrapper findContentCachingWrapper(HttpServletResponse response) {
		HttpServletResponse current = response;

		// Traverse the wrapper chain to find ContentCachingResponseWrapper
		while (current != null) {
			if (current instanceof ContentCachingResponseWrapper) {
				return (ContentCachingResponseWrapper) current;
			}
			// Try to unwrap if it's a wrapper
			if (current instanceof HttpServletResponseWrapper) {
				HttpServletResponse wrapped = (HttpServletResponse) ((HttpServletResponseWrapper) current).getResponse();
				current = wrapped;
			} else {
				break;
			}
		}

		log.warn("ContentCachingResponseWrapper not found in chain. Current type: {}", response.getClass().getName());
		return null;
	}

	private void handleNonCachingResponse(HttpServletResponse response, HttpServletRequest request) {
		log.warn("Cannot modify response - ContentCachingResponseWrapper not available");
		log.warn("Response type: {}", response.getClass().getName());

		// You could try alternative approaches here, such as:
		// 1. Setting response headers to indicate processing
		// 2. Logging the issue for debugging
		// 3. Using a different interception strategy
	}


	/*@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
	                            Object handler, Exception ex) throws Exception {

		if (isNexlConfigEndpoint(request) && response.getStatus() == 200) {
			log.info("Config response intercepted for NEXL: {}", request.getRequestURI());
			if (response instanceof ContentCachingResponseWrapper) {
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
		} else {
			// handle or log the case where response is not ContentCachingResponseWrapper
			log.warn("Response is not ContentCachingResponseWrapper, but: {}", response.getClass().getName());
		}
	}
*/
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
