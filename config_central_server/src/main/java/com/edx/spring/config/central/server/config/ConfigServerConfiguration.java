package com.edx.spring.config.central.server.config;

import com.edx.spring.config.central.server.CustomEntryPointEnvironmentRepository;
import com.edx.spring.config.central.server.env.CustomMultipleJGitEnvironmentRepository;
import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentProperties;
import org.springframework.cloud.config.server.environment.ObservationEnvironmentRepositoryWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
@Profile("operation")
@Slf4j
@EnableConfigServer
public class ConfigServerConfiguration {

	@Bean
	@ConfigurationProperties("spring.cloud.config.server.git")
	@ConditionalOnProperty(name = "spring.cloud.config.server.git.enabled", havingValue = "true")
	public MultipleJGitEnvironmentProperties gitProperties() {
		return new MultipleJGitEnvironmentProperties();
	}

	// Custom Git repository that handles non-Git labels gracefully
	@Bean
	@ConditionalOnProperty(name = "spring.cloud.config.server.git.enabled", havingValue = "true")
	public EnvironmentRepository gitEnvironmentRepository(
			ConfigurableEnvironment springEnv,
			MultipleJGitEnvironmentProperties gitProps,
			ObservationRegistry observationRegistry) {

		log.info("Creating CustomMultipleJGitEnvironmentRepository");
		CustomMultipleJGitEnvironmentRepository customGitRepo =
				new CustomMultipleJGitEnvironmentRepository(springEnv, gitProps, observationRegistry);

		return ObservationEnvironmentRepositoryWrapper.wrap(observationRegistry, customGitRepo);
	}

	// Nexl repository for handling nexl-specific requests
	@Bean
	public EnvironmentRepository nexlEnvironmentRepository(List<ConfigResourceProvider> providers) {
		log.info("Creating Nexl EnvironmentRepository");
		// Use your existing CustomEntryPointEnvironmentRepository but without Git delegation
		return new CustomEntryPointEnvironmentRepository(providers, null);
	}

	// Let Spring create the CompositeEnvironmentRepository automatically
	// It will pick up both gitEnvironmentRepository and nexlEnvironmentRepository beans

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		return restTemplate;
	}
}
