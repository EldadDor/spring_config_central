package com.edx.spring.config.central.server.config;

import com.edx.spring.config.central.server.CustomEntryPointEnvironmentRepository;
import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.ObservationEnvironmentRepositoryWrapper;
import org.springframework.context.annotation.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
@Profile("custom-repo")
public class ConfigServerConfiguration {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE) // Add this line
    public EnvironmentRepository environmentRepository(ObservationRegistry observationRegistry, CustomEntryPointEnvironmentRepository customEntryPointEnvironmentRepository) {
        return ObservationEnvironmentRepositoryWrapper.wrap(observationRegistry, customEntryPointEnvironmentRepository);
    }

    @Bean(name = "CustomEntryPointEnvironmentRepository")
    public CustomEntryPointEnvironmentRepository customEntryPointEnvironmentRepository(List<ConfigResourceProvider> providers) {
        return new CustomEntryPointEnvironmentRepository(providers); // This will now be the only way it's created
    }
}
