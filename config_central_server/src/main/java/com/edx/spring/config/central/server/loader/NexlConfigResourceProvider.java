package com.edx.spring.config.central.server.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class NexlConfigResourceProvider implements ConfigResourceProvider {

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${config.providers.nexl.enabled:true}")
	private boolean enabled;

	@Value("${config.providers.nexl.base-url:http://nexl:8181/deployment/javaserver}")
	private String baseUrl;

	@Override
	public boolean supports(String label) {
		// Support when label is "nexl" or when it's the primary provider
		return enabled && ("nexl".equals(label) || "nexl-primary".equals(label));
	}

	@Override
	public Map<String, Object> loadProperties(String application, String profile, String label) {
		log.info("NexlConfigResourceProvider loading properties for application: {}, profile: {}, label: {}",
				application, profile, label);

		if (!enabled) {
			log.info("Nexl provider is disabled");
			return new HashMap<>();
		}

		try {
			// Build the URL to your Nexl server
			String url = baseUrl + "/" + application + "-" + profile + ".json";
			log.info("Fetching configuration from Nexl: {}", url);

			// Fetch the JSON from your Nexl server
			String json = restTemplate.getForObject(url, String.class);

			if (json != null && !json.trim().isEmpty()) {
				// Parse JSON to Map
				Map<String, Object> properties = objectMapper.readValue(json, Map.class);
				log.info("Successfully loaded {} properties from Nexl", properties.size());
				return properties;
			} else {
				log.warn("No configuration found at Nexl URL: {}", url);
				return new HashMap<>();
			}

		} catch (Exception e) {
			log.error("Failed to load configuration from Nexl server: {}", e.getMessage(), e);
			// Return empty map instead of throwing exception to allow fallback
			return new HashMap<>();
		}
	}

	@Override
	public int getOrder() {
		return 1; // Higher priority than Git
	}
}