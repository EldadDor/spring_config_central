package com.edx.spring.config.central.server.loader;

import com.edx.spring.config.central.server.KNexlService;
import com.edx.spring.config.central.server.admin.ConfigProviderManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import java.net.URLEncoder;

@Component
@Slf4j
public class NexlConfigResourceProvider implements HttpRequestAwareConfigResourceProvider {

	@Autowired
	private ConfigProviderManager providerManager;
	@Autowired
	private KNexlService nexlService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${config.providers.nexl.enabled:true}")
	private boolean enabled;


	@Value("${config.providers.nexl.base-url:http://nexl:8181}")
	private String baseUrl;

	@Value("${config.providers.nexl.fallback:false}")
	private boolean fallback;

	/*@Override
	public boolean supports(String label) {
		// Support when label is "nexl" or when it's the primary provider
		return enabled && ("nexl".equals(label) || "nexl-primary".equals(label));
	}*/

	@Override
	public Map<String, Object> loadProperties(String application, String profile, String label, HttpServletRequest request) {
		log.info("NexlConfigResourceProvider loading properties with HTTP request context");

		if (!enabled) {
			log.info("Nexl provider is disabled");
			return new HashMap<>();
		}
		try {
			String path;
			String expression;

			if (request != null) {
				// Extract path and expression from HTTP request
				String[] pathAndExpression = extractPathAndExpressionFromHttpRequest(request);
				path = pathAndExpression[0];
				expression = pathAndExpression[1];
				log.info("Extracted from HTTP request - path: {}, expression: {}", path, expression);
			} else {
				// Fallback to parameter-based path and expression extraction
				String[] pathAndExpression = extractPathAndExpressionFromParameters(application, profile);
				path = pathAndExpression[0];
				expression = pathAndExpression[1];
				log.info("Extracted from parameters - path: {}, expression: {}", path, expression);
			}

			KNexlService.NexlResult nexlResult = nexlService.callNexlServerForJava(path, expression);

			if (nexlResult.isSuccess()) {
				String response = nexlResult.getData();
				log.info("Nexl server response body length: {}", response != null ? response.length() : 0);

				if (response != null && !response.trim().isEmpty()) {
					log.debug("Nexl server response body: {}", response);
					return parseNexlResponse(response, application, profile, request);
				} else {
					log.warn("No configuration found for path: {} with expression: {}", path, expression);
					return new HashMap<>();
				}
			} else {
				Throwable failure = nexlResult.getException();
				log.error("Failed to load configuration from Nexl server: {}",
						failure != null ? failure.getMessage() : "Unknown error", failure);
			}

		} catch (Exception e) {
			log.error("Failed to load configuration from Nexl server: {}", e.getMessage(), e);
		}

		return new HashMap<>();
	}


	@Override
	public boolean supports(String label) {
		// 1) Check if provider is enabled
		if (!enabled) {
			log.debug("Nexl provider disabled via property");
			return false;
		}

		// 2) Explicit labels should always work when enabled
		if ("nexl".equals(label) || "nexl-primary".equals(label)) {
			log.debug("Supporting explicit nexl label: {}", label);
			return true;
		}

		// 3) Fallback behavior - only if enabled and not a git label
		if (!fallback) {
			return false;
		}

		if (isGitLabel(label)) {
			return false;
		}

		// 4) Check provider manager for dynamic enablement (optional)
		boolean managerEnabled = providerManager == null ||
				providerManager.isProviderEnabled(this.getClass().getSimpleName());
		boolean nexlIsPrimary = providerManager == null ||
				"nexl".equals(providerManager.getPrimaryProvider());

		return managerEnabled && nexlIsPrimary;
	}

	private boolean isGitLabel(String label) {
		return "git".equals(label) ||
				"main".equals(label) ||
				"master".equals(label) ||
				"develop".equals(label) ||
				label.startsWith("feature/") ||
				label.startsWith("release/") ||
				label.matches("v\\d+\\.\\d+.*");
	}

	private boolean isPrimaryProvider() {
		return true; // or check some configuration property
	}

	private String[] extractPathAndExpressionFromHttpRequest(HttpServletRequest request) {
		String requestURI = request.getRequestURI();
		String queryString = request.getQueryString();

		log.info("Processing request URI: {}", requestURI);
		log.info("Query string: {}", queryString);

		// Check if there's a 'url' parameter - this is the primary case
		String urlParam = request.getParameter("url");
		if (urlParam != null) {
			return splitUrlParameterIntoPathAndExpression(urlParam);
		}

		// Fallback to path-based URL building
		return extractPathAndExpressionFromRequestPath(requestURI, queryString);
	}

/*	private String[] splitUrlParameterIntoPathAndExpression(String urlParam) {
		log.info("Found URL parameter: {}", urlParam);

		// Split on "expression="
		int expressionIndex = urlParam.indexOf("expression=");
		if (expressionIndex == -1) {
			// No expression parameter, return path only
			return new String[]{urlParam, ""};
		}

		// Split the URL at the expression parameter
		String path = urlParam.substring(0, expressionIndex - 1); // Remove the "?" before "expression="
		String expressionPart = urlParam.substring(expressionIndex + "expression=".length());

		// Find the end of the expression value (next & or end of string)
		int nextParamIndex = expressionPart.indexOf('&');
		String expression = nextParamIndex != -1 ?
				expressionPart.substring(0, nextParamIndex) :
				expressionPart;

		// URL decode the expression since it comes from the URL parameter
		String decodedExpression = URLDecoder.decode(expression, StandardCharsets.UTF_8);

		log.info("Split URL - path: {}, expression: {}", path, decodedExpression);
		return new String[]{path, decodedExpression};
	}*/

	private String[] splitUrlParameterIntoPathAndExpression(String urlParam) {
		log.info("Found URL parameter: {}", urlParam);

		// Check if URL contains expression parameter
		int expressionIndex = urlParam.indexOf("expression=");
		if (expressionIndex != -1) {
			// Handle the case with expression parameter (existing logic)
			String path = urlParam.substring(0, expressionIndex - 1); // Remove the "?" before "expression="
			String expressionPart = urlParam.substring(expressionIndex + "expression=".length());

			// Find the end of the expression value (next & or end of string)
			int nextParamIndex = expressionPart.indexOf('&');
			String expression = nextParamIndex != -1 ?
					expressionPart.substring(0, nextParamIndex) :
					expressionPart;

			// URL decode the expression since it comes from the URL parameter
			String decodedExpression = URLDecoder.decode(expression, StandardCharsets.UTF_8);

			log.info("Split URL with expression - path: {}, expression: {}", path, decodedExpression);
			return new String[]{path, decodedExpression};
		} else {
			// Handle the case without expression parameter - just pass the entire URL as path
			log.info("URL without expression parameter - using entire URL as path: {}", urlParam);
			return new String[]{urlParam, ""};
		}
	}

	private String[] extractPathAndExpressionFromRequestPath(String requestURI, String queryString) {
		// Parse the Spring Cloud Config URL pattern: /{application}/{profile}/{label}
		String[] pathParts = requestURI.split("/");
		if (pathParts.length < 4) {
			throw new IllegalArgumentException("Invalid request URI format: " + requestURI);
		}

		String application = URLDecoder.decode(pathParts[1], StandardCharsets.UTF_8);
		String profile = URLDecoder.decode(pathParts[2], StandardCharsets.UTF_8);

		log.info("Decoded application: {}", application);
		log.info("Decoded profile: {}", profile);

		// Build path and extract expression
		String path = buildPathFromApplicationProfile(application, profile);
		String expression = extractExpressionFromQueryOrProfile(queryString, profile);

		return new String[]{path, expression};
	}

	private String[] extractPathAndExpressionFromParameters(String application, String profile) {
		log.info("Building path and expression from parameters - application: {}, profile: {}", application, profile);

		String path;
		String expression = "";

		if (application.contains("/") && application.endsWith(".js")) {
			// Direct path specified (e.g., "java-opts/docker-conf/mobile.js")
			path = "/" + application;

			// Check if profile contains expression
			if (profile.contains("expression=")) {
				expression = extractExpressionFromProfile(profile);
			}
		} else if (profile.contains("?")) {
			// Query parameters in profile (e.g., "js?expression=${all}")
			String[] parts = profile.split("\\?", 2);
			path = "/" + application + "/" + parts[0];

			// Extract expression from query part
			if (parts[1].contains("expression=")) {
				String[] expressionParts = parts[1].split("expression=", 2);
				if (expressionParts.length > 1) {
					expression = URLDecoder.decode(expressionParts[1].split("&")[0], StandardCharsets.UTF_8);
				}
			}
		} else {
			// Standard application/profile format
			path = "/" + application + "/" + profile + ".js";
		}

		return new String[]{path, expression};
	}

	private String buildPathFromApplicationProfile(String application, String profile) {
		if (application.endsWith(".js")) {
			// Direct JavaScript file path
			return "/" + application;
		} else if (profile.contains("?")) {
			// Profile contains query parameters (e.g., "js?expression=${all}")
			String[] profileParts = profile.split("\\?", 2);
			return "/" + application + "/" + profileParts[0];
		} else {
			// Standard pattern
			return "/" + application + "/" + profile + ".js";
		}
	}

	private String extractExpressionFromQueryOrProfile(String queryString, String profile) {
		// Try query string first
		if (queryString != null && queryString.contains("expression=")) {
			String[] parts = queryString.split("expression=", 2);
			if (parts.length > 1) {
				return URLDecoder.decode(parts[1].split("&")[0], StandardCharsets.UTF_8);
			}
		}

		// Try profile
		return extractExpressionFromProfile(profile);
	}

	@Override
	public Map<String, Object> loadProperties(String application, String profile, String label) {
		// Fallback method without HTTP request context
		return loadProperties(application, profile, label, null);
	}

	private String buildNexlUrlFromHttpRequest(HttpServletRequest request) {
		String requestURI = request.getRequestURI();
		String queryString = request.getQueryString();

		log.info("Processing request URI: {}", requestURI);
		log.info("Query string: {}", queryString);

		// Check if there's a 'url' parameter - this is the primary case
		String urlParam = request.getParameter("url");
		if (urlParam != null) {
			return buildNexlUrlFromUrlParameterWithReEncoding(urlParam);
		}

		// Fallback to path-based URL building
		return buildNexlUrlFromRequestPath(requestURI, queryString);
	}

	private String buildNexlUrlFromUrlParameter(String urlParam) {
		log.info("Found URL parameter: {}", urlParam);

		// Let's try without re-encoding first - the parameter is already decoded by the servlet
		String finalUrl = baseUrl + urlParam;

		log.info("Built final URL: {}", finalUrl);
		return finalUrl;
	}

	// Keep the re-encoding method but don't use it by default - we can test both approaches
	private String buildNexlUrlFromUrlParameterWithReEncoding(String urlParam) {
		log.info("Found URL parameter: {}", urlParam);

		// Re-encode only the expression part if it exists
		String processedUrl = reEncodeExpressionInUrl(urlParam);
		String finalUrl = baseUrl + processedUrl;

		log.info("Built final URL with re-encoding: {}", finalUrl);
		return finalUrl;
	}

	private String reEncodeExpressionInUrl(String url) {
		// Check if URL contains expression parameter
		int expressionIndex = url.indexOf("expression=");
		if (expressionIndex == -1) {
			return url; // No expression parameter, return as-is
		}

		// Split the URL at the expression parameter
		String beforeExpression = url.substring(0, expressionIndex + "expression=".length());
		String afterExpression = url.substring(expressionIndex + "expression=".length());

		// Find the end of the expression value (next & or end of string)
		int nextParamIndex = afterExpression.indexOf('&');
		String expressionValue;
		String remainingParams;

		if (nextParamIndex != -1) {
			expressionValue = afterExpression.substring(0, nextParamIndex);
			remainingParams = afterExpression.substring(nextParamIndex);
		} else {
			expressionValue = afterExpression;
			remainingParams = "";
		}

		// Re-encode only the expression value
		String encodedExpression = URLEncoder.encode(expressionValue, StandardCharsets.UTF_8);

		return beforeExpression + encodedExpression + remainingParams;
	}

	private String buildNexlUrlFromRequestPath(String requestURI, String queryString) {
		// Parse the Spring Cloud Config URL pattern: /{application}/{profile}/{label}
		String[] pathParts = requestURI.split("/");
		if (pathParts.length < 4) {
			throw new IllegalArgumentException("Invalid request URI format: " + requestURI);
		}

		String application = URLDecoder.decode(pathParts[1], StandardCharsets.UTF_8);
		String profile = URLDecoder.decode(pathParts[2], StandardCharsets.UTF_8);

		log.info("Decoded application: {}", application);
		log.info("Decoded profile: {}", profile);

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

		// Build path based on different patterns
		buildNexlPath(builder, application, profile);

		// Add query parameters from original request
		if (queryString != null) {
			addQueryParametersFromString(builder, queryString);
		}

		return builder.build().toUriString();
	}

	private void buildNexlPath(UriComponentsBuilder builder, String application, String profile) {
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
//			return properties;
			// Add metadata about the source
			return properties;
			/*properties.put("nexl.source.application", application);
			properties.put("nexl.source.profile", profile);
			properties.put("nexl.source.timestamp", System.currentTimeMillis());

			if (request != null) {
				properties.put("nexl.source.requestUri", request.getRequestURI());
				properties.put("nexl.source.queryString", request.getQueryString());
			}*/

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