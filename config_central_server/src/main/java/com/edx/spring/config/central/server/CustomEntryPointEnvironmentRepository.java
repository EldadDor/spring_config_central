package com.edx.spring.config.central.server;

import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import com.edx.spring.config.central.server.loader.HttpRequestAwareConfigResourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Slf4j
public class CustomEntryPointEnvironmentRepository implements EnvironmentRepository {

	private final List<ConfigResourceProvider> providers;

	public CustomEntryPointEnvironmentRepository(List<ConfigResourceProvider> providers) {
		this.providers = providers.stream()
				.sorted(AnnotationAwareOrderComparator.INSTANCE)
				.toList();
		log.info("Initialized with {} providers: {}", providers.size(),
				providers.stream()
						.map(p -> p.getClass().getSimpleName() + "(order=" + p.getOrder() + ")")
						.toList());
	}

	@Override
	public Environment findOne(String application, String profile, String label) {
		log.info("Finding configuration for application: {}, profile: {}, label: {}",
				application, profile, label);

		// Get the current HTTP request
		HttpServletRequest request = getCurrentHttpRequest();
		if (request != null) {
			log.info("Request URL: {}", request.getRequestURL());
			log.info("Query String: {}", request.getQueryString());
			log.info("Request Headers: {}", getHeadersAsString(request));
		}

		Environment environment = new Environment(application, new String[]{profile}, label, null, null);

		// Try each provider in order
		for (ConfigResourceProvider provider : providers) {
			if (provider.supports(label)) {
				log.info("Using provider: {} for label: {}", provider.getClass().getSimpleName(), label);

				try {
					Map<String, Object> properties;

					// If provider supports HTTP request, pass it
					if (provider instanceof HttpRequestAwareConfigResourceProvider) {
						properties = ((HttpRequestAwareConfigResourceProvider) provider)
								.loadProperties(application, profile, label, request);
					} else {
						properties = provider.loadProperties(application, profile, label);
					}

					if (properties != null && !properties.isEmpty()) {
						String sourceName = provider.getClass().getSimpleName() + "-" + label;
						environment.add(new PropertySource(sourceName, properties));
						log.info("Added {} properties from {}", properties.size(), sourceName);
					}
				} catch (Exception e) {
					log.error("Provider {} failed to load properties", provider.getClass().getSimpleName(), e);
				}
			}
		}

		log.info("Returning environment with {} property sources", environment.getPropertySources().size());
		return environment;
	}

	private HttpServletRequest getCurrentHttpRequest() {
		try {
			ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
			return attributes.getRequest();
		} catch (IllegalStateException e) {
			log.debug("No HTTP request context available: {}", e.getMessage());
			return null;
		}
	}

	private String getHeadersAsString(HttpServletRequest request) {
		StringBuilder headers = new StringBuilder();
		request.getHeaderNames().asIterator().forEachRemaining(name ->
				headers.append(name).append("=").append(request.getHeader(name)).append("; "));
		return headers.toString();
	}
}