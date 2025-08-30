package com.edx.spring.config.central.server.config;

import com.edx.spring.config.central.server.CustomEntryPointEnvironmentRepository;
import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.ObservationEnvironmentRepositoryWrapper;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;


@Configuration
@Profile("custom-repo")
public class ConfigServerConfiguration {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public EnvironmentRepository environmentRepository(
			ObservationRegistry observationRegistry,
			List<ConfigResourceProvider> providers
	) {
		// Create the delegate here so it's NOT registered as its own EnvironmentRepository bean
		CustomEntryPointEnvironmentRepository delegate = new CustomEntryPointEnvironmentRepository(providers);
		return ObservationEnvironmentRepositoryWrapper.wrap(observationRegistry, delegate);
	}


	@Bean
	public RestTemplate restTemplate() {
		RestTemplate restTemplate = new RestTemplate();

		// Add message converters if needed
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

		return restTemplate;
	}
}

