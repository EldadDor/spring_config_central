package com.edx.spring.config.central.server;

import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import com.edx.spring.config.central.server.loader.HttpRequestAwareConfigResourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentRepository;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

@Slf4j
public class CustomEntryPointEnvironmentRepository implements EnvironmentRepository {

	private final List<ConfigResourceProvider> providers;
	private final MultipleJGitEnvironmentRepository gitEnvironmentRepository;

	// Original constructor (backward compatibility)
	public CustomEntryPointEnvironmentRepository(List<ConfigResourceProvider> providers) {
		this(providers, null);
	}

	// New constructor with Git support
	public CustomEntryPointEnvironmentRepository(
			List<ConfigResourceProvider> providers,
			MultipleJGitEnvironmentRepository gitEnvironmentRepository) {

		this.providers = providers.stream()
				.sorted(AnnotationAwareOrderComparator.INSTANCE)
				.toList();
		this.gitEnvironmentRepository = gitEnvironmentRepository;

		log.info("Initialized with {} providers: {}", providers.size(),
				providers.stream()
						.map(p -> p.getClass().getSimpleName() + "(order=" + p.getOrder() + ")")
						.toList());

		if (gitEnvironmentRepository != null) {
			log.info("Git repository support enabled");
		} else {
			log.info("Git repository support disabled - only provider-based configuration available");
		}
	}

	@Override
	public Environment findOne(String application, String profile, String label) {
		log.info("Finding configuration for application: {}, profile: {}, label: {}",
				application, profile, label);

		// Check if request explicitly asks for Git
		if (gitEnvironmentRepository != null && isExplicitGitRequest(label)) {
			log.info("Routing to Git repository for explicit Git label: {}", label);
			return gitEnvironmentRepository.findOne(application, profile, label);
		}

		// Check if request explicitly asks for Nexl
		if (isExplicitNexlRequest(label)) {
			log.info("Routing to Nexl provider for label: {}", label);
			return findOneUsingProviders(application, profile, label);
		}

		// Default routing based on label pattern
		if (gitEnvironmentRepository != null && isGitBranchPattern(label)) {
			log.info("Routing to Git repository for branch pattern: {}", label);
			return gitEnvironmentRepository.findOne(application, profile, label);
		}

		// Default to providers (Nexl)
		return findOneUsingProviders(application, profile, label);
	}

	private boolean isExplicitNexlRequest(String label) {
		return "nexl".equals(label) || "nexl-primary".equals(label);
	}

	private boolean isExplicitGitRequest(String label) {
		return "git".equals(label);
	}

	private boolean isGitBranchPattern(String label) {
		if (label == null) return false;

		return "main".equals(label) ||
				"master".equals(label) ||
				"develop".equals(label) ||
				label.startsWith("feature/") ||
				label.startsWith("release/") ||
				label.matches("v\\d+\\.\\d+.*");
	}


	private Environment findOneUsingProviders(String application, String profile, String label) {
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
						environment.setName("");
						environment.setLabel("");
						environment.setVersion("");
						environment.add(new PropertySource(null, properties));
						return environment;
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
