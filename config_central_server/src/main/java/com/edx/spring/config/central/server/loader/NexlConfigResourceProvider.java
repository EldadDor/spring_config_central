package com.edx.spring.config.central.server.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class NexlConfigResourceProvider implements HttpRequestAwareConfigResourceProvider {

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${config.providers.nexl.enabled:true}")
	private boolean enabled;

	@Value("${config.providers.nexl.base-url:http://nexl:8181}")
	private String baseUrl;

	@Override
	public boolean supports(String label) {
		// Support when label is "nexl" or when it's the primary provider
		return enabled && ("nexl".equals(label) || "nexl-primary".equals(label));
	}

	@Override
	public Map<String, Object> loadProperties(String application, String profile, String label, HttpServletRequest request) {
		log.info("NexlConfigResourceProvider loading properties with HTTP request context");

		if (!enabled) {
			log.info("Nexl provider is disabled");
			return new HashMap<>();
		}

		try {
			String nexlUrl;

			if (request != null) {
				// Build URL from actual HTTP request
				nexlUrl = buildNexlUrlFromHttpRequest(request);
				log.info("Built Nexl URL from HTTP request: {}", nexlUrl);
			} else {
				// Fallback to parameter-based URL building
				nexlUrl = buildNexlUrlFromParameters(application, profile);
				log.info("Built Nexl URL from parameters: {}", nexlUrl);
			}

			String response = restTemplate.getForObject(nexlUrl, String.class);

			if (response != null && !response.trim().isEmpty()) {
				return parseNexlResponse(response, application, profile, request);
			} else {
				log.warn("No configuration found at Nexl URL: {}", nexlUrl);
				return new HashMap<>();
			}

		} catch (Exception e) {
			log.error("Failed to load configuration from Nexl server: {}", e.getMessage(), e);
		}

		return new HashMap<>();
	}

	@Override
	public Map<String, Object> loadProperties(String application, String profile, String label) {
		// Fallback method without HTTP request context
		return loadProperties(application, profile, label, null);
	}

	private String buildNexlUrlFromHttpRequest(HttpServletRequest request) {
		// Extract the path from the request URL
		String requestURI = request.getRequestURI();
		String queryString = request.getQueryString();

		log.info("Processing request URI: {}", requestURI);
		log.info("Query string: {}", queryString);

		// Parse the Spring Cloud Config URL pattern: /{application}/{profile}/{label}
		String[] pathParts = requestURI.split("/");
		if (pathParts.length >= 4) {
			String application = URLDecoder.decode(pathParts[1], StandardCharsets.UTF_8);
			String profile = URLDecoder.decode(pathParts[2], StandardCharsets.UTF_8);

			log.info("Decoded application: {}", application);
			log.info("Decoded profile: {}", profile);

			// Build Nexl URL
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

			// Handle different URL patterns
			if (application.endsWith(".js")) {
				// Direct JavaScript file path
				builder.path("/" + application);
			} else if (profile.contains("?")) {
				// Profile contains query parameters (e.g., "js?expression=${all}")
				String[] profileParts = profile.split("\\?", 2);
				builder.path("/" + application + "/" + profileParts[0]);

				// Add query parameters from profile
				if (profileParts.length > 1) {
					addQueryParametersFromString(builder, profileParts[1]);
				}
			} else {
				// Standard pattern
				builder.path("/" + application + "/" + profile + ".js");
			}

			// Add query parameters from original request
			if (queryString != null) {
				addQueryParametersFromString(builder, queryString);
			}

			return builder.build().toUriString();
		}

		throw new IllegalArgumentException("Invalid request URI format: " + requestURI);
	}

	private String buildNexlUrlFromParameters(String application, String profile) {
		log.info("Building Nexl URL from parameters - application: {}, profile: {}", application, profile);

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

		// Parse different patterns
		if (application.contains("/") && application.endsWith(".js")) {
			// Direct path specified (e.g., "java-opts/docker-conf/mobile.js")
			builder.path("/" + application);

			// Check if profile contains expression
			if (profile.contains("expression=")) {
				String expression = extractExpressionFromProfile(profile);
				if (expression != null) {
					builder.queryParam("expression", expression);
				}
			}
		} else if (profile.contains("?")) {
			// Query parameters in profile (e.g., "js?expression=${all}")
			String[] parts = profile.split("\\?", 2);
			builder.path("/" + application + "/" + parts[0]);
			addQueryParametersFromString(builder, parts[1]);
		} else {
			// Standard application/profile format
			builder.path("/" + application + "/" + profile + ".js");
		}

		return builder.build().toUriString();
	}

	private void addQueryParametersFromString(UriComponentsBuilder builder, String queryString) {
		if (queryString != null && !queryString.trim().isEmpty()) {
			String[] params = queryString.split("&");
			for (String param : params) {
				String[] keyValue = param.split("=", 2);
				if (keyValue.length == 2) {
					String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
					String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
					builder.queryParam(key, value);
				}
			}
		}
	}

	private String extractExpressionFromProfile(String profile) {
		if (profile.contains("expression=")) {
			String[] parts = profile.split("expression=", 2);
			if (parts.length > 1) {
				return URLDecoder.decode(parts[1].split("&")[0], StandardCharsets.UTF_8);
			}
		}
		return null;
	}

	private Map<String, Object> parseNexlResponse(String response, String application, String profile, HttpServletRequest request) {
		Map<String, Object> properties = new HashMap<>();

		try {
			// If the response looks like JSON, parse it as JSON
			if (response.trim().startsWith("{") || response.trim().startsWith("[")) {
				properties = objectMapper.readValue(response, Map.class);
				log.info("Successfully parsed JSON response with {} properties", properties.size());
			} else {
				// For JavaScript files, try to extract JSON or key-value pairs
				properties = parseJavaScriptResponse(response);
				log.info("Parsed JavaScript response with {} properties", properties.size());
			}

			// Add metadata about the source
			properties.put("nexl.source.application", application);
			properties.put("nexl.source.profile", profile);
			properties.put("nexl.source.timestamp", System.currentTimeMillis());

			if (request != null) {
				properties.put("nexl.source.requestUri", request.getRequestURI());
				properties.put("nexl.source.queryString", request.getQueryString());
			}

		} catch (Exception e) {
			log.warn("Failed to parse Nexl response as JSON, treating as plain text: {}", e.getMessage());
			// Fallback: store the raw response
			properties.put("nexl.raw.response", response);
			properties.put("nexl.source.application", application);
			properties.put("nexl.source.profile", profile);
		}

		return properties;
	}

	private Map<String, Object> parseJavaScriptResponse(String jsResponse) {
		Map<String, Object> properties = new HashMap<>();

		try {
			// Look for JSON-like structures in the JS response
			String[] lines = jsResponse.split("\n");
			for (String line : lines) {
				line = line.trim();
				// Simple key-value extraction for JavaScript variable assignments
				if (line.contains("=") && !line.startsWith("//") && !line.startsWith("/*")) {
					String[] parts = line.split("=", 2);
					if (parts.length == 2) {
						String key = parts[0].trim().replaceAll("^(var|let|const)\\s+", "");
						String value = parts[1].trim().replaceAll(";$", "").replaceAll("\"", "");
						properties.put(key, value);
					}
				}
			}

			// Try to find and extract JSON blocks
			if (properties.isEmpty()) {
				int jsonStart = jsResponse.indexOf("{");
				int jsonEnd = jsResponse.lastIndexOf("}");
				if (jsonStart >= 0 && jsonEnd > jsonStart) {
					String jsonPart = jsResponse.substring(jsonStart, jsonEnd + 1);
					try {
						Map<String, Object> jsonProperties = objectMapper.readValue(jsonPart, Map.class);
						properties.putAll(jsonProperties);
						log.debug("Extracted JSON block from JavaScript response");
					} catch (Exception e) {
						log.debug("Failed to parse JSON from JS response: {}", e.getMessage());
					}
				}
			}

		} catch (Exception e) {
			log.warn("Failed to parse JavaScript response: {}", e.getMessage());
		}

		return properties;
	}

	@Override
	public int getOrder() {
		return 1; // Higher priority than Git
	}
}