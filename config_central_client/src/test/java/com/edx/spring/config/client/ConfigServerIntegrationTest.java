package com.edx.spring.config.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.config.uri=http://localhost:8888",
    "spring.cloud.config.label=nexl",
    "spring.application.name=test-app",
    "spring.profiles.active=development"
})
class ConfigServerIntegrationTest {

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void testConfigServerDirectAccess() {
        // Test direct access to config server
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:8888/test-app/development/nexl", Map.class);
        
        System.out.println("Config Server Response: " + response.getBody());
        
        // The response should contain the configuration from your NexlConfigResourceProvider
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void testConfigServerEnvironmentEndpoint() {
        // Test the environment endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8888/test-app-development.json", String.class);
        
        System.out.println("Environment Response: " + response.getBody());
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
