
package com.edx.spring.config.central.server;

import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.List;
import java.util.Map;

@Slf4j
public class CustomEntryPointEnvironmentRepository implements EnvironmentRepository {

    private final List<ConfigResourceProvider> providers;

    public CustomEntryPointEnvironmentRepository(List<ConfigResourceProvider> providers) {
        // Sort providers by order annotation/interface
        this.providers = providers.stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .toList();
        log.info("Initialized with {} providers: {}",
                providers.size(),
                providers.stream()
                        .map(p -> p.getClass().getSimpleName() + "(order=" + p.getOrder() + ")")
                        .toList());
    }

    @Override
    public Environment findOne(String application, String profile, String label) {
        log.info("Finding configuration for application: {}, profile: {}, label: {}",
                application, profile, label);

        Environment environment = new Environment(application, new String[]{profile}, label, null, null);

        // Try each provider in order
        for (ConfigResourceProvider provider : providers) {
            if (provider.supports(label)) {
                log.info("Using provider: {} for label: {}", provider.getClass().getSimpleName(), label);

                try {
                    Map<String, Object> properties = provider.loadProperties(application, profile, label);
                    if (properties != null && !properties.isEmpty()) {
                        String sourceName = provider.getClass().getSimpleName() + "-" + label;
                        environment.add(new PropertySource(sourceName, properties));
                        log.info("Added {} properties from {}", properties.size(), sourceName);
                    }
                } catch (Exception e) {
                    log.error("Provider {} failed to load properties", provider.getClass().getSimpleName(), e);
                }
            }
        }

        // If no properties were loaded, try fallback providers
        if (environment.getPropertySources().isEmpty()) {
            log.warn("No properties loaded from primary providers, trying fallback providers");

            for (ConfigResourceProvider provider : providers) {
                if (provider.supports("fallback") || provider.supports("default")) {
                    log.info("Using fallback provider: {}", provider.getClass().getSimpleName());

                    try {
                        Map<String, Object> properties = provider.loadProperties(application, profile, "fallback");
                        if (properties != null && !properties.isEmpty()) {
                            String sourceName = provider.getClass().getSimpleName() + "-fallback";
                            environment.add(new PropertySource(sourceName, properties));
                            log.info("Added {} fallback properties from {}", properties.size(), sourceName);
                            break; // Use first successful fallback
                        }
                    } catch (Exception e) {
                        log.error("Fallback provider {} failed to load properties", provider.getClass().getSimpleName(), e);
                    }
                }
            }
        }

        log.info("Returning environment with {} property sources", environment.getPropertySources().size());
        return environment;
    }
}