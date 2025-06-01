package com.edx.spring.config.central.loader;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NexlConfigResourceProvider implements ConfigResourceProvider {
    @Override
    public boolean supports(String label) {
        return false;
    }

    @Override
    public Map<String, Object> loadProperties(String application, String profile, String label) {
        return Map.of();
    }
}
