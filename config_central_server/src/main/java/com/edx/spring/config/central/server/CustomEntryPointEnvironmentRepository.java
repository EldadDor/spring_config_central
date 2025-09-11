
package com.edx.spring.config.central.server;

import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import com.edx.spring.config.central.server.loader.HttpRequestAwareConfigResourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentRepository;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;


@Slf4j
@Component
public class CustomEntryPointEnvironmentRepository implements EnvironmentRepository {

	private final List<ConfigResourceProvider> providers;
	private final MultipleJGitEnvironmentRepository gitEnvironmentRepository;

	// Constructor that Spring will use for dependency injection
	public CustomEntryPointEnvironmentRepository(
			List<ConfigResourceProvider> providers,
			@Autowired(required = false) MultipleJGitEnvironmentRepository gitEnvironmentRepository) {

		this.providers = providers.stream()
				.sorted(AnnotationAwareOrderComparator.INSTANCE)
				.toList();
		this.gitEnvironmentRepository = gitEnvironmentRepository;

		log.info("=== CustomEntryPointEnvironmentRepository initialized ===");
		log.info("Providers count: {}", providers.size());
		providers.forEach(p -> log.info("  - {} (order={})", p.getClass().getSimpleName(), p.getOrder()));

		if (gitEnvironmentRepository != null) {
			log.info("Git repository support: ENABLED");
		} else {
			log.info("Git repository support: DISABLED");
		}
	}

	@Override
	public Environment findOne(String application, String profile, String label) {
		log.info("=== REQUEST RECEIVED ===");
		log.info("Application: {}, Profile: {}, Label: {}", application, profile, label);

		// Check if request explicitly asks for Git
		if (gitEnvironmentRepository != null && isExplicitGitRequest(label)) {
			log.info("ROUTING TO: Git repository (explicit git label)");
			return gitEnvironmentRepository.findOne(application, profile, label);
		}

		// Check if request explicitly asks for Nexl
		if (isExplicitNexlRequest(label)) {
			log.info("ROUTING TO: Nexl providers (explicit nexl label)");
			return findOneUsingProviders(application, profile, label);
		}

		// Default routing based on label pattern
		if (gitEnvironmentRepository != null && isGitBranchPattern(label)) {
			log.info("ROUTING TO: Git repository (branch pattern match)");
			return gitEnvironmentRepository.findOne(application, profile, label);
		}

		// Default to providers (Nexl)
		log.info("ROUTING TO: Providers (default)");
		return findOneUsingProviders(application, profile, label);
	}

	private boolean isExplicitNexlRequest(String label) {
		boolean isNexl = "nexl".equals(label) || "nexl-primary".equals(label);
		log.debug("isExplicitNexlRequest('{}') = {}", label, isNexl);
		return isNexl;
	}

	private boolean isExplicitGitRequest(String label) {
		boolean isGit = "git".equals(label);
		log.debug("isExplicitGitRequest('{}') = {}", label, isGit);
		return isGit;
	}

	private boolean isGitBranchPattern(String label) {
		if (label == null) return false;

		boolean isGitPattern = "main".equals(label) ||
				"master".equals(label) ||
				"develop".equals(label) ||
				label.startsWith("feature/") ||
				label.startsWith("release/") ||
				label.matches("v\\d+\\.\\d+.*");

		log.debug("isGitBranchPattern('{}') = {}", label, isGitPattern);
		return isGitPattern;
	}

	private Environment findOneUsingProviders(String application, String profile, String label) {
		log.info("=== USING PROVIDERS ===");

		// Get the current HTTP request
		HttpServletRequest request = getCurrentHttpRequest();
		if (request != null) {
			log.info("Request URL: {}", request.getRequestURL());
			log.info("Query String: {}", request.getQueryString());
		} else {
			log.warn("No HTTP request context available");
		}

		Environment environment = new Environment(application, new String[]{profile}, label, null, null);

		// Try each provider in order
		for (ConfigResourceProvider provider : providers) {
			log.info("Checking provider: {}", provider.getClass().getSimpleName());

			if (provider.supports(label)) {
				log.info(">>> USING PROVIDER: {} for label: {}", provider.getClass().getSimpleName(), label);

				try {
					Map<String, Object> properties;

					// If provider supports HTTP request, pass it
					if (provider instanceof HttpRequestAwareConfigResourceProvider) {
						log.info("Using HTTP-aware provider");
						properties = ((HttpRequestAwareConfigResourceProvider) provider)
								.loadProperties(application, profile, label, request);
					} else {
						log.info("Using standard provider");
						properties = provider.loadProperties(application, profile, label);
					}

					if (properties != null && !properties.isEmpty()) {
						String sourceName = provider.getClass().getSimpleName() + "-" + label;
						environment.add(new PropertySource(sourceName, properties));
						log.info("Added {} properties from {}", properties.size(), sourceName);

						// Return immediately after first successful provider
						log.info("=== RETURNING ENVIRONMENT with {} property sources ===", environment.getPropertySources().size());
						return environment;
					} else {
						log.warn("Provider {} returned empty properties", provider.getClass().getSimpleName());
					}
				} catch (Exception e) {
					log.error("Provider {} failed: {}", provider.getClass().getSimpleName(), e.getMessage(), e);
				}
			} else {
				log.debug("Provider {} does not support label '{}'", provider.getClass().getSimpleName(), label);
			}
		}

		log.info("=== NO PROVIDERS MATCHED - returning empty environment ===");
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