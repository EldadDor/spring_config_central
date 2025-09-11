package com.edx.spring.config.central.server.config;

import com.edx.spring.config.central.server.CustomEntryPointEnvironmentRepository;
import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.config.server.config.ConfigServerAutoConfiguration;
import org.springframework.cloud.config.server.config.ConfigServerEncryptionConfiguration;
import org.springframework.cloud.config.server.config.ConfigServerMvcConfiguration;
import org.springframework.cloud.config.server.environment.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
@Profile("operation")
@Slf4j
public class ConfigServerConfiguration {

	@Bean
	@ConfigurationProperties("spring.cloud.config.server.git")
	@ConditionalOnProperty(name = "spring.cloud.config.server.git.enabled", havingValue = "true")
	public MultipleJGitEnvironmentProperties gitProperties() {
		return new MultipleJGitEnvironmentProperties();
	}

	// THE ONLY EnvironmentRepository bean
	@Bean
	@Primary
	public EnvironmentRepository environmentRepository(
			ObservationRegistry observationRegistry,
			ConfigurableEnvironment springEnv,
			@Autowired(required = false) MultipleJGitEnvironmentProperties gitProps,
			List<ConfigResourceProvider> providers) {

		log.info("=== Creating THE ONLY EnvironmentRepository bean ===");

		// Create Git repository internally (not as a bean)
		MultipleJGitEnvironmentRepository gitRepo = null;
		if (gitProps != null) {
			log.info("Initializing embedded MultipleJGitEnvironmentRepository");
			gitRepo = new MultipleJGitEnvironmentRepository(springEnv, gitProps, observationRegistry);
		} else {
			log.info("Git repository support: DISABLED");
		}

		CustomEntryPointEnvironmentRepository delegate =
				new CustomEntryPointEnvironmentRepository(providers, gitRepo);

		EnvironmentRepository wrapped = ObservationEnvironmentRepositoryWrapper.wrap(observationRegistry, delegate);
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
