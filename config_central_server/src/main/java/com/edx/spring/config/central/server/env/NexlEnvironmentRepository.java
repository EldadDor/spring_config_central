package com.edx.spring.config.central.server.env;

import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import com.edx.spring.config.central.server.loader.HttpRequestAwareConfigResourceProvider;
import com.edx.spring.config.central.server.loader.NexlConfigResourceProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Custom EnvironmentRepository for Nexl backend. This implementation delegates to the NexlConfigResourceProvider
 * to load properties, handling label-based routing and HTTP request awareness via RequestContextHolder.
 * It returns an Environment with PropertySources populated from the provider, or an empty Environment if not supported.
 */
@Slf4j
public class NexlEnvironmentRepository implements EnvironmentRepository {

	private final NexlConfigResourceProvider nexlProvider;
	// If you have multiple providers for Nexl, you can inject a list; otherwise, just the main one
	private final List<ConfigResourceProvider> additionalProviders;

	@Autowired
	public NexlEnvironmentRepository(NexlConfigResourceProvider nexlProvider,
	                                 @Autowired(required = false) List<ConfigResourceProvider> additionalProviders) {
		this.nexlProvider = nexlProvider;
		this.additionalProviders = additionalProviders != null ? additionalProviders : List.of();
	}

	@Override
	public Environment findOne(String application, String profile, String label) {
		log.info("NexlEnvironmentRepository: Finding configuration for application: {}, profile: {}, label: {}",
				application, profile, label);

		// Check if Nexl supports this label/request
		if (!nexlProvider.supports(label)) {
			log.debug("Nexl does not support label: {}. Returning empty Environment.", label);
			return new Environment(application, profile); // Empty Environment for non-matching requests
		}

		// Get the current HTTP request via RequestContextHolder (for request-aware loading)
		HttpServletRequest request = getCurrentRequest();
		if (request == null) {
			log.warn("No HTTP request context available. Falling back to request-unaware loading.");
		}

		// Load properties from the main Nexl provider
		Map<String, Object> properties = nexlProvider.loadProperties(application, profile, label, request);

		// Optionally load from additional providers (merge if needed)
		for (ConfigResourceProvider provider : additionalProviders) {
			if (provider.supports(label)) {
				if (provider instanceof HttpRequestAwareConfigResourceProvider awareProvider) {
					properties.putAll(awareProvider.loadProperties(application, profile, label, request));
				} else {
					properties.putAll(provider.loadProperties(application, profile, label));
				}
			}
		}

		if (properties.isEmpty()) {
			log.info("No properties loaded for Nexl. Returning empty Environment.");
			return new Environment(application, profile);
		}

		// Create and populate the Environment
		Environment environment = new Environment(application, profile, label, null, null);
		PropertySource propertySource = new PropertySource("nexl:" + application + "-" + profile + "-" + (label != null ? label : "default"), properties);
		environment.add(propertySource);

		log.info("NexlEnvironmentRepository loaded {} properties for {} / {} / {}",
				properties.size(), application, profile, label);
		return environment;
	}

	private HttpServletRequest getCurrentRequest() {
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
			return attributes.getRequest();
		} catch (IllegalStateException e) {
			log.debug("No request attributes available: {}", e.getMessage());
			return null;
		}
	}
}
