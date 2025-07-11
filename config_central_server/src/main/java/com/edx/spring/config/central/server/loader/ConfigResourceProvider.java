
package com.edx.spring.config.central.server.loader;

import org.springframework.core.Ordered;

import java.util.Map;

public interface ConfigResourceProvider extends Ordered {

    boolean supports(String label);

    Map<String, Object> loadProperties(String application, String profile, String label);

    @Override
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE; // Default to lowest priority
    }
}