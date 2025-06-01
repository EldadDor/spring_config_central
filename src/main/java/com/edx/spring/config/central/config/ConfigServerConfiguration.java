package com.edx.spring.config.central.config;

import com.edx.spring.config.central.CustomEntryPointEnvironmentRepository;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.ObservationEnvironmentRepositoryWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;


@Configuration
@Profile("custom-repo") // Ensures this config is active only when 'custom-repo' profile is active
public class ConfigServerConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // Add this line
    public EnvironmentRepository environmentRepository(ObservationRegistry observationRegistry, CustomEntryPointEnvironmentRepository customEntryPointEnvironmentRepository) {
        return ObservationEnvironmentRepositoryWrapper.wrap(observationRegistry, customEntryPointEnvironmentRepository);
    }

    @Bean(name = "CustomEntryPointEnvironmentRepository")
    public CustomEntryPointEnvironmentRepository customEntryPointEnvironmentRepository() {
        return new CustomEntryPointEnvironmentRepository(); // This will now be the only way it's created
    }
}
