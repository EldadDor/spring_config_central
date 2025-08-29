package com.edx.spring.config.central.server.loader;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

public interface HttpRequestAwareConfigResourceProvider extends ConfigResourceProvider {

	Map<String, Object> loadProperties(String application, String profile, String label, HttpServletRequest request);

	@Override
	default Map<String, Object> loadProperties(String application, String profile, String label) {
		// Fallback to request-unaware method
		return loadProperties(application, profile, label, null);
	}
}