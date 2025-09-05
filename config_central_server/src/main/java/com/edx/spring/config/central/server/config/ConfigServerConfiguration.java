package com.edx.spring.config.central.server.config;

import com.edx.spring.config.central.server.CustomEntryPointEnvironmentRepository;
import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentRepository;
import org.springframework.cloud.config.server.environment.MultipleJGitEnvironmentProperties;
import org.springframework.cloud.config.server.environment.ObservationEnvironmentRepositoryWrapper;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
@Profile("operation")
@EnableConfigServer
@Import({}) // Explicitly empty to avoid auto-imports
public class ConfigServerConfiguration {

	@Bean
	@ConfigurationProperties("spring.cloud.config.server.git")
	@ConditionalOnProperty(name = "spring.cloud.config.server.git.enabled", havingValue = "true")
	public MultipleJGitEnvironmentProperties gitProperties() {
		return new MultipleJGitEnvironmentProperties();
	}

	@Bean
	@ConditionalOnProperty(name = "spring.cloud.config.server.git.enabled", havingValue = "true")
	public MultipleJGitEnvironmentRepository gitEnvironmentRepository(
			ConfigurableEnvironment environment,
			MultipleJGitEnvironmentProperties gitProperties,
			ObservationRegistry observationRegistry) {

		return new MultipleJGitEnvironmentRepository(environment, gitProperties, observationRegistry);
	}

	// Remove @Primary and use a different bean name to avoid conflicts
	@Bean(name = "customEnvironmentRepository")
	public EnvironmentRepository customEnvironmentRepository(
			ObservationRegistry observationRegistry,
			List<ConfigResourceProvider> providers,
			@Autowired(required = false) MultipleJGitEnvironmentRepository gitEnvironmentRepository
	) {
		// Create the delegate with optional Git support
		CustomEntryPointEnvironmentRepository delegate =
				new CustomEntryPointEnvironmentRepository(providers, gitEnvironmentRepository);
		return ObservationEnvironmentRepositoryWrapper.wrap(observationRegistry, delegate);
	}

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		return restTemplate;
	}
}
