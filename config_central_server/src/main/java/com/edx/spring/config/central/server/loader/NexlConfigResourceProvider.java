package com.edx.spring.config.central.server.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class NexlConfigResourceProvider implements ConfigResourceProvider {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${config.providers.nexl.enabled:true}")
    private boolean enabled;

    @Value("${config.providers.nexl.base-url:http://nexl:8181/deployment/javaserver}")
    private String baseUrl;

    @Value("${config.providers.nexl.endpoint:http://nexl:8181/jenkins/deployment/micro-services/deploy-ms.js}")
    private String endpoint;


    @Override
    public boolean supports(String label) {
        // Support when label is "nexl" or when it's the primary provider
        return enabled && ("nexl".equals(label) || "nexl-primary".equals(label));
    }

    @Override
    public Map<String, Object> loadProperties(String application, String profile, String label) {
        log.info("NexlConfigResourceProvider loading properties for application={}, profile={}, label={}",
                application, profile, label);

        if (!enabled) {
            log.info("Nexl provider is disabled");
            return new HashMap<>();
        }
        try {
            HttpServletRequest request = currentRequest();
            // Whitelist and read the params you want to forward to Nexl
            String expression = request != null ? request.getParameter("expression") : null;
            // Build the Nexl URL with safe encoding
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(endpoint);
            if (expression != null && !expression.isBlank()) {
                builder.queryParam("expression", expression); // encoded by builder
            }

            String url = builder.toUriString();
            log.info("Calling Nexl URL={}", url);

            // Call Nexl and parse the response as JSON -> Map<String, Object>
            String json = restTemplate.getForObject(url, String.class);
            if (json == null || json.isBlank()) {
                log.warn("Empty response from Nexl");
                return new HashMap<>();
            }

            Map<String, Object> properties = objectMapper.readValue(json, Map.class);
            log.info("Loaded {} properties from Nexl", properties.size());
            return properties;

        } catch (Exception e) {
            log.error("Failure calling Nexl error={}", e.getMessage(), e);
            return new HashMap<>();
        }

    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception ignored) {
            return null;
        }
    }


    @Override
    public int getOrder() {
        return 1; // Higher priority than Git
    }
}