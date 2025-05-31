package com.edx.spring.config.central;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class UrlJsonEnvironmentRepository implements EnvironmentRepository {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Environment findOne(String application, String profile, String label) {
        try {
            // Build your URL dynamically as needed
            String url = "http://nexl:8181/deployment/javaserver/" + application + "-" + profile + ".json";
            String json = restTemplate.getForObject(url, String.class);
            Map<String, Object> properties = objectMapper.readValue(json, Map.class);

            Environment environment = new Environment(application, new String[]{profile}, label, null, null);
            environment.add(new PropertySource("url-json", properties));
            return environment;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config from URL", e);
        }
    }
}
