/*
 * User: eadno1
 * Date: 13/09/2025
 *
 * Copyright (2005) IDI. All rights reserved.
 * This software is a proprietary information of Israeli Direct Insurance.
 * Created by IntelliJ IDEA.
 */
package com.edx.spring.config.central.server.env;

/**
 *
 */
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentProperties;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentRepository;
import org.springframework.core.env.ConfigurableEnvironment;

@Slf4j
public class CustomMultipleJGitEnvironmentRepository extends MultipleJGitEnvironmentRepository {

	public CustomMultipleJGitEnvironmentRepository(ConfigurableEnvironment environment,
	                                               MultipleJGitEnvironmentProperties properties,
	                                               ObservationRegistry observationRegistry) {
		super(environment, properties, observationRegistry);
		log.info("CustomMultipleJGitEnvironmentRepository initialized");
	}

	@Override
	public Environment findOne(String application, String profile, String label, boolean includeOrigin) {
		log.info("CustomGitRepo: Processing request - App: {}, Profile: {}, Label: {}", application, profile, label);

		// Handle non-Git labels by returning empty Environment
		if (isNonGitLabel(label)) {
			log.info("Non-Git label '{}' detected. Returning empty Environment.", label);
			return new Environment(application, new String[]{profile}, label, null, null);
		}

		// For Git-compatible labels, delegate to parent (standard Git logic)
		log.info("Git-compatible label '{}'. Delegating to parent Git repository.", label);
		try {
			return super.findOne(application, profile, label, includeOrigin);
		} catch (Exception e) {
			log.warn("Git repository failed for label '{}': {}. Returning empty Environment.", label, e.getMessage());
			return new Environment(application, new String[]{profile}, label, null, null);
		}
	}

	@Override
	public Environment findOne(String application, String profile, String label) {
		return findOne(application, profile, label, false);
	}

	private boolean isNonGitLabel(String label) {
		if (label == null) {
			return false;
		}

		// Explicit non-Git labels
		if ("nexl".equals(label) || "nexl-primary".equals(label)) {
			return true;
		}

		// Add other non-Git patterns as needed
		// e.g., if (label.startsWith("nexl-")) return true;

		return false;
	}
}
