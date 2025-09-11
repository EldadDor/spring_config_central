package com.edx.spring.config.central.server.config;

import com.edx.spring.config.central.server.CustomEntryPointEnvironmentRepository;
import com.edx.spring.config.central.server.env.NexlEnvironmentRepository;
import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Autowired;
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
@Configuration
@Profile("operation")
@EnableConfigServer
public class ConfigServerConfiguration {

	// Git properties bound to configuration (e.g., from application.yml)
	@Bean
	@ConfigurationProperties("spring.cloud.config.server.git")
	@ConditionalOnProperty(name = "spring.cloud.config.server.git.enabled", havingValue = "true")
	public MultipleJGitEnvironmentProperties gitProperties() {
		return new MultipleJGitEnvironmentProperties();
	}

	// Spring's native Git EnvironmentRepository
	@Bean
	@ConditionalOnProperty(name = "spring.cloud.config.server.git.enabled", havingValue = "true")
	public EnvironmentRepository gitEnvironmentRepository(
			ConfigurableEnvironment environment,
			MultipleJGitEnvironmentProperties gitProperties,
			ObservationRegistry observationRegistry) {
		return ObservationEnvironmentRepositoryWrapper.wrap(
				observationRegistry,
				new MultipleJGitEnvironmentRepository(environment, gitProperties, observationRegistry)
		);
	}

	// The primary composite repository that combines Git and Nexl
	@Bean
	@Primary
	public EnvironmentRepository environmentRepository(ObservationRegistry observationRegistry, @Autowired(required = false) EnvironmentRepository gitEnvironmentRepository,
	                                                   NexlEnvironmentRepository nexlEnvironmentRepository) {

		// List of repositories to compose (Git is optional based on condition)
		List<EnvironmentRepository> repositories = gitEnvironmentRepository != null
				? Arrays.asList(gitEnvironmentRepository, nexlEnvironmentRepository)
				: List.of(nexlEnvironmentRepository);

		// Create composite
		CompositeEnvironmentRepository composite = new CompositeEnvironmentRepository(repositories, observationRegistry, true);

		// Wrap for observability
		return ObservationEnvironmentRepositoryWrapper.wrap(observationRegistry, composite);
	}

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		return restTemplate;
	}
}
