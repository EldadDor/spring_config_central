package com.edx.spring.config.central;

import com.edx.spring.config.central.loader.ConfigResourceProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class CustomEntryPointEnvironmentRepository implements EnvironmentRepository {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<ConfigResourceProvider> providers;

    public CustomEntryPointEnvironmentRepository(List<ConfigResourceProvider> providers) {
        this.providers = providers;
    }
    @Override
    public Environment findOne(String application, String profile, String label) {
        try {
            // Build your URL dynamically as needed
            for (ConfigResourceProvider provider : providers) {
                if (provider.supports(label)) {
                    Map<String, Object> properties = provider.loadProperties(application, profile, label);
                    Environment environment = new Environment(application, new String[]{profile}, label, null, null);
                    environment.add(new PropertySource(label, properties));
                    return environment;
                }
            }
            throw new IllegalArgumentException("No provider found for label: " + label);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load config from URL", e);
        }
    }



/*
            String url = "http://nexl:8181/deployment/javaserver/" + application + "-" + profile + ".json";
            String json = restTemplate.getForObject(url, String.class);
            Map<String, Object> properties = objectMapper.readValue(json, Map.class);

            Environment environment = new Environment(application, new String[]{profile}, label, null, null);
            environment.add(new PropertySource("url-json", properties));
            return environment;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config from URL", e);
        }
    }*/
}
