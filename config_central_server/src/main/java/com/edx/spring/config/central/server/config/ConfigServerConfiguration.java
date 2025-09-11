package com.edx.spring.config.central.server.config;

import com.edx.spring.config.central.server.CustomEntryPointEnvironmentRepository;
import com.edx.spring.config.central.server.env.NexlEnvironmentRepository;
import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.config.CompositeConfiguration;
import org.springframework.cloud.config.server.config.EnvironmentRepositoryConfiguration;
import org.springframework.cloud.config.server.environment.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for Spring Cloud Config Server, enabling both Git and Nexl backends
 * via a composite repository. This reuses Spring's native Git support while integrating
 * custom Nexl logic.
 */


/**
 * Configuration class for Spring Cloud Config Server, enabling both Git and Nexl backends
 * via a custom repository with smart routing logic.
 */
@Configuration
@Profile("operation")
@Slf4j
@EnableConfigServer
public class ConfigServerConfiguration {

	// Git properties bound to configuration (e.g., from application.yml)
	@Bean
	@ConfigurationProperties("spring.cloud.config.server.git")
	@ConditionalOnProperty(name = "spring.cloud.config.server.git.enabled", havingValue = "true")
	public MultipleJGitEnvironmentProperties gitProperties() {
		return new MultipleJGitEnvironmentProperties();
	}

	// Create the raw Git repository (not exposed as EnvironmentRepository)
	@Bean("multipleJGitEnvironmentRepository")
	@ConditionalOnProperty(name = "spring.cloud.config.server.git.enabled", havingValue = "true")
	public MultipleJGitEnvironmentRepository multipleJGitEnvironmentRepository(
			ConfigurableEnvironment environment,
			MultipleJGitEnvironmentProperties gitProperties,
			ObservationRegistry observationRegistry) {
		return new MultipleJGitEnvironmentRepository(environment, gitProperties, observationRegistry);
	}

	// THE ONLY EnvironmentRepository bean - your custom entry point
	@Bean
	@Primary
	public EnvironmentRepository environmentRepository(
			ObservationRegistry observationRegistry,
			CustomEntryPointEnvironmentRepository customEntryPointRepository) {

		log.info("=== Creating THE ONLY EnvironmentRepository bean ===");
		log.info("Using CustomEntryPointRepository: {}", customEntryPointRepository.getClass().getName());

		EnvironmentRepository wrapped = ObservationEnvironmentRepositoryWrapper.wrap(observationRegistry, customEntryPointRepository);
		log.info("Created wrapped repository: {}", wrapped.getClass().getName());
		return wrapped;
	}

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		return restTemplate;
	}
}