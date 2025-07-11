package com.edx.spring.config.central.server.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class GitConfigResourceProvider implements ConfigResourceProvider {

    @Value("${config.providers.git.enabled:true}")
    private boolean enabled;

    @Value("${config.providers.git.fallback:true}")
    private boolean fallback;

    @Override
    public boolean supports(String label) {
        // Support when label is "git", "master", "main", or when used as fallback
        return enabled && ("git".equals(label) || "master".equals(label) ||
                "main".equals(label) || fallback);
    }

    @Override
    public Map<String, Object> loadProperties(String application, String profile, String label) {
        log.info("GitConfigResourceProvider loading properties for application: {}, profile: {}, label: {}",
                application, profile, label);

        if (!enabled) {
            log.info("Git provider is disabled");
            return new HashMap<>();
        }

        // For now, return some default properties
        // In a real implementation, you would integrate with Git repositories
        Map<String, Object> properties = new HashMap<>();
        properties.put("source", "git");
        properties.put("git.branch", label);
        properties.put("git.application", application);
        properties.put("git.profile", profile);

        // Add some sample configuration
        properties.put("database.pool.size", "10");
        properties.put("cache.enabled", "true");
        properties.put("logging.level", "INFO");

        log.info("Git provider loaded {} properties", properties.size());
        return properties;
    }

    @Override
    public int getOrder() {
        return 2; // Lower priority than Nexl
    }
}