package com.edx.spring.config.central.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/config")
@RefreshScope
@Slf4j
public class ConfigTestController {

    private final Environment environment;

    public ConfigTestController(Environment environment) {
        this.environment = environment;
    }

    @Value("${test.message:default-message}")
    private String testMessage;

    @Value("${test.number:42}")
    private int testNumber;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @GetMapping("/test")
    public Map<String, Object> getConfig() {
        log.info("Getting configuration values");
        
        Map<String, Object> config = new HashMap<>();
        config.put("applicationName", applicationName);
        config.put("testMessage", testMessage);
        config.put("testNumber", testNumber);
        config.put("activeProfiles", environment.getActiveProfiles());
        
        return config;
    }

    @GetMapping("/properties")
    public Map<String, Object> getAllProperties() {
        log.info("Getting all configuration properties");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Add some common properties that might be configured
        properties.put("test.message", environment.getProperty("test.message", "not-found"));
        properties.put("test.number", environment.getProperty("test.number", "not-found"));
        properties.put("database.url", environment.getProperty("database.url", "not-found"));
        properties.put("database.username", environment.getProperty("database.username", "not-found"));
        properties.put("custom.property", environment.getProperty("custom.property", "not-found"));
        
        return properties;
    }

    @GetMapping("/refresh")
    public String refreshConfig() {
        log.info("Configuration refresh requested");
        return "Configuration refreshed at " + System.currentTimeMillis();
    }
}
