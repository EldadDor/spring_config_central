package com.edx.spring.config.central.loader;

import java.util.Map;

public interface ConfigResourceProvider {
    boolean supports(String label);

    Map<String, Object> loadProperties(String application, String profile, String label);
}
