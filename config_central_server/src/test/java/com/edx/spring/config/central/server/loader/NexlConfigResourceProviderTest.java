package com.edx.spring.config.central.server.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NexlConfigResourceProviderTest {

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private HttpServletRequest httpRequest;

	private NexlConfigResourceProvider provider;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		provider = new NexlConfigResourceProvider();
		objectMapper = new ObjectMapper();

		// Inject mocked RestTemplate and set up configuration
		ReflectionTestUtils.setField(provider, "restTemplate", restTemplate);
		ReflectionTestUtils.setField(provider, "objectMapper", objectMapper);
		ReflectionTestUtils.setField(provider, "enabled", true);
		ReflectionTestUtils.setField(provider, "baseUrl", "http://nexl:8181");
	}

	@Test
	void shouldSupportNexlLabels() {
		assertThat(provider.supports("nexl")).isTrue();
		assertThat(provider.supports("nexl-primary")).isTrue();
		assertThat(provider.supports("git")).isFalse();
		assertThat(provider.supports("vault")).isFalse();
	}

	@Test
	void shouldLoadPropertiesWithHttpRequestContext() {
		// Given - Mock HTTP request for: http://nexl:8181/java-opts/docker-conf/mobile.js
		when(httpRequest.getRequestURI()).thenReturn("/java-opts%2Fdocker-conf%2Fmobile.js/default/nexl");
		when(httpRequest.getQueryString()).thenReturn(null);

		String expectedUrl = "http://nexl:8181/java-opts/docker-conf/mobile.js";
		String mockResponse = """
                {
                    "app.name": "mobile-app",
                    "server.port": "8080",
                    "database.url": "jdbc:mysql://localhost:3306/mobile"
                }
                """;

		when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn(mockResponse);

		// When
		Map<String, Object> properties = provider.loadProperties(
				"java-opts/docker-conf/mobile.js", "default", "nexl", httpRequest);

		// Then
		assertThat(properties).isNotEmpty();
		assertThat(properties).containsEntry("app.name", "mobile-app");
		assertThat(properties).containsEntry("server.port", "8080");
		assertThat(properties).containsEntry("database.url", "jdbc:mysql://localhost:3306/mobile");
		assertThat(properties).containsEntry("nexl.source.application", "java-opts/docker-conf/mobile.js");
		assertThat(properties).containsEntry("nexl.source.profile", "default");
		assertThat(properties).containsEntry("nexl.source.requestUri", "/java-opts%2Fdocker-conf%2Fmobile.js/default/nexl");
		assertThat(properties).containsKey("nexl.source.timestamp");
	}

	@Test
	void shouldLoadPropertiesWithQueryParametersFromHttpRequest() {
		// Given - Mock HTTP request for: http://nexl:8181/jenkins/deployment/micro-services/deploy-ms.js?expression=${all}
		when(httpRequest.getRequestURI()).thenReturn("/jenkins%2Fdeployment%2Fmicro-services%2Fdeploy-ms/js%3Fexpression%3D%24%7Ball%7D/nexl");
		when(httpRequest.getQueryString()).thenReturn(null);

		String expectedUrl = "http://nexl:8181/jenkins/deployment/micro-services/deploy-ms.js?expression=%24%7Ball%7D";
		String mockResponse = """
                {
                    "microservices": {
                        "user-service": {
                            "port": 8081,
                            "replicas": 2
                        },
                        "order-service": {
                            "port": 8082,
                            "replicas": 3
                        }
                    },
                    "database": {
                        "host": "db.example.com",
                        "port": 5432
                    }
                }
                """;

		when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn(mockResponse);

		// When
		Map<String, Object> properties = provider.loadProperties(
				"jenkins/deployment/micro-services/deploy-ms", "js?expression=${all}", "nexl", httpRequest);

		// Then
		assertThat(properties).isNotEmpty();
		assertThat(properties).containsKey("microservices");
		assertThat(properties).containsKey("database");
		assertThat(properties).containsEntry("nexl.source.application", "jenkins/deployment/micro-services/deploy-ms");
		assertThat(properties).containsEntry("nexl.source.profile", "js?expression=${all}");
	}

	@Test
	void shouldLoadPropertiesWithInternalAPIExpression() {
		// Given - Mock HTTP request for: http://nexl:8181/jenkins/deployment/micro-services/deploy-ms.js?expression=${internalAPI}
		when(httpRequest.getRequestURI()).thenReturn("/jenkins%2Fdeployment%2Fmicro-services%2Fdeploy-ms/js%3Fexpression%3D%24%7BinternalAPI%7D/nexl");
		when(httpRequest.getQueryString()).thenReturn(null);

		String expectedUrl = "http://nexl:8181/jenkins/deployment/micro-services/deploy-ms.js?expression=%24%7BinternalAPI%7D";
		String mockResponse = """
                {
                    "internal-api": {
                        "gateway": {
                            "url": "https://internal-api.company.com",
                            "timeout": 30000
                        },
                        "auth": {
                            "type": "oauth2",
                            "clientId": "internal-client"
                        }
                    }
                }
                """;

		when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn(mockResponse);

		// When
		Map<String, Object> properties = provider.loadProperties(
				"jenkins/deployment/micro-services/deploy-ms", "js?expression=${internalAPI}", "nexl", httpRequest);

		// Then
		assertThat(properties).isNotEmpty();
		assertThat(properties).containsKey("internal-api");
		assertThat(properties).containsEntry("nexl.source.application", "jenkins/deployment/micro-services/deploy-ms");
		assertThat(properties).containsEntry("nexl.source.profile", "js?expression=${internalAPI}");
	}

	@Test
	void shouldLoadPropertiesWithComplexUsertestExpression() {
		// Given - Mock HTTP request for: http://nexl:8181/jenkins/deployment/micro-services/deploy-ms.js?expression=${all.clientpolicies.crm.envs.USERTEST}
		when(httpRequest.getRequestURI()).thenReturn("/jenkins%2Fdeployment%2Fmicro-services%2Fdeploy-ms/js%3Fexpression%3D%24%7Ball.clientpolicies.crm.envs.USERTEST%7D/nexl");
		when(httpRequest.getQueryString()).thenReturn(null);

		String expectedUrl = "http://nexl:8181/jenkins/deployment/micro-services/deploy-ms.js?expression=%24%7Ball.clientpolicies.crm.envs.USERTEST%7D";
		String mockResponse = """
                {
                    "usertest": {
                        "database": {
                            "url": "jdbc:postgresql://usertest-db:5432/crm",
                            "username": "test_user",
                            "password": "test_pass"
                        },
                        "cache": {
                            "enabled": true,
                            "ttl": 300
                        },
                        "logging": {
                            "level": "DEBUG"
                        }
                    }
                }
                """;

		when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn(mockResponse);

		// When
		Map<String, Object> properties = provider.loadProperties(
				"jenkins/deployment/micro-services/deploy-ms", "js?expression=${all.clientpolicies.crm.envs.USERTEST}", "nexl", httpRequest);

		// Then
		assertThat(properties).isNotEmpty();
		assertThat(properties).containsKey("usertest");
		Map<String, Object> usertest = (Map<String, Object>) properties.get("usertest");
		assertThat(usertest).containsKey("database");
		assertThat(usertest).containsKey("cache");
		assertThat(usertest).containsKey("logging");
		assertThat(properties).containsEntry("nexl.source.application", "jenkins/deployment/micro-services/deploy-ms");
		assertThat(properties).containsEntry("nexl.source.profile", "js?expression=${all.clientpolicies.crm.envs.USERTEST}");
	}

	@Test
	void shouldFallbackToParameterBasedUrlBuilding() {
		// Given - No HTTP request context, fallback to parameter-based building
		String expectedUrl = "http://nexl:8181/java-opts/docker-conf/mobile.js";
		String mockResponse = """
                {
                    "fallback": "parameter-based",
                    "config.value": "test"
                }
                """;

		when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn(mockResponse);

		// When
		Map<String, Object> properties = provider.loadProperties(
				"java-opts/docker-conf/mobile.js", "default", "nexl", null);

		// Then
		assertThat(properties).isNotEmpty();
		assertThat(properties).containsEntry("fallback", "parameter-based");
		assertThat(properties).containsEntry("config.value", "test");
		assertThat(properties).containsEntry("nexl.source.application", "java-opts/docker-conf/mobile.js");
		assertThat(properties).containsEntry("nexl.source.profile", "default");
		// Should not contain request-specific metadata
		assertThat(properties).doesNotContainKey("nexl.source.requestUri");
		assertThat(properties).doesNotContainKey("nexl.source.queryString");
	}

	@Test
	void shouldHandleJavaScriptResponseWithVariables() {
		// Given
		String expectedUrl = "http://nexl:8181/java-opts/docker-conf/mobile.js";
		String mockJsResponse = """
                // Mobile application configuration
                var appName = "mobile-app";
                var serverPort = "8080";
                var debugMode = true;
                
                var config = {
                    name: "mobile",
                    version: "1.0.0",
                    features: {
                        pushNotifications: true,
                        analytics: false
                    }
                };
                
                module.exports = config;
                """;

		when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn(mockJsResponse);

		// When
		Map<String, Object> properties = provider.loadProperties(
				"java-opts/docker-conf/mobile.js", "default", "nexl", null);

		// Then
		assertThat(properties).isNotEmpty();
		assertThat(properties).containsEntry("nexl.source.application", "java-opts/docker-conf/mobile.js");
		assertThat(properties).containsEntry("nexl.source.profile", "default");
		assertThat(properties).containsKey("nexl.source.timestamp");

		// Should extract some variables or JSON from the JavaScript
		// The exact content depends on the parsing logic, but should not be empty
		assertThat(properties.size()).isGreaterThan(3); // At least the metadata properties
	}

	@Test
	void shouldHandleStandardPathPattern() {
		// Given - Standard application/profile pattern
		String expectedUrl = "http://nexl:8181/myapp/production.js";
		String mockResponse = """
                {
                    "environment": "production",
                    "database": {
                        "host": "prod-db.company.com",
                        "port": 5432,
                        "name": "myapp_prod"
                    },
                    "logging": {
                        "level": "INFO"
                    }
                }
                """;

		when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn(mockResponse);

		// When
		Map<String, Object> properties = provider.loadProperties("myapp", "production", "nexl", null);

		// Then
		assertThat(properties).isNotEmpty();
		assertThat(properties).containsEntry("environment", "production");
		assertThat(properties).containsKey("database");
		assertThat(properties).containsKey("logging");
		assertThat(properties).containsEntry("nexl.source.application", "myapp");
		assertThat(properties).containsEntry("nexl.source.profile", "production");
	}

	@Test
	void shouldHandleEmptyResponse() {
		// Given
		String expectedUrl = "http://nexl:8181/java-opts/docker-conf/mobile.js";
		when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn("");

		// When
		Map<String, Object> properties = provider.loadProperties(
				"java-opts/docker-conf/mobile.js", "default", "nexl", null);

		// Then
		assertThat(properties).isEmpty();
	}

	@Test
	void shouldHandleNullResponse() {
		// Given
		String expectedUrl = "http://nexl:8181/java-opts/docker-conf/mobile.js";
		when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn(null);

		// When
		Map<String, Object> properties = provider.loadProperties(
				"java-opts/docker-conf/mobile.js", "default", "nexl", null);

		// Then
		assertThat(properties).isEmpty();
	}

	@Test
	void shouldHandleRestTemplateException() {
		// Given
		String expectedUrl = "http://nexl:8181/java-opts/docker-conf/mobile.js";
		when(restTemplate.getForObject(expectedUrl, String.class))
				.thenThrow(new RuntimeException("Connection timeout"));

		// When
		Map<String, Object> properties = provider.loadProperties(
				"java-opts/docker-conf/mobile.js", "default", "nexl", null);

		// Then
		assertThat(properties).isEmpty();
	}

	@Test
	void shouldHandleInvalidJsonResponse() {
		// Given
		String expectedUrl = "http://nexl:8181/java-opts/docker-conf/mobile.js";
		String invalidJsonResponse = "{ invalid json response without closing brace";

		when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn(invalidJsonResponse);

		// When
		Map<String, Object> properties = provider.loadProperties(
				"java-opts/docker-conf/mobile.js", "default", "nexl", null);

		// Then
		assertThat(properties).isNotEmpty();
		assertThat(properties).containsEntry("nexl.raw.response", invalidJsonResponse);
		assertThat(properties).containsEntry("nexl.source.application", "java-opts/docker-conf/mobile.js");
		assertThat(properties).containsEntry("nexl.source.profile", "default");
	}

	@Test
	void shouldReturnEmptyMapWhenProviderIsDisabled() {
		// Given
		ReflectionTestUtils.setField(provider, "enabled", false);

		// When
		Map<String, Object> properties = provider.loadProperties(
				"java-opts/docker-conf/mobile.js", "default", "nexl", null);

		// Then
		assertThat(properties).isEmpty();
	}

	@Test
	void shouldUseHttpRequestContextWhenAvailable() {
		// Given - Both HTTP request and parameters provided
		when(httpRequest.getRequestURI()).thenReturn("/test/app/profile/nexl");
		when(httpRequest.getQueryString()).thenReturn("expression=%24%7Ball%7D");

		String expectedUrl = "http://nexl:8181/test/profile.js?expression=%24%7Ball%7D";
		String mockResponse = """
                {
                    "source": "http-request",
                    "value": "from-request"
                }
                """;

		when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn(mockResponse);

		// When - HTTP request should take precedence over parameters
		Map<String, Object> properties = provider.loadProperties(
				"different-app", "different-profile", "nexl", httpRequest);

		// Then
		assertThat(properties).isNotEmpty();
		assertThat(properties).containsEntry("source", "http-request");
		assertThat(properties).containsEntry("nexl.source.requestUri", "/test/app/profile/nexl");
		assertThat(properties).containsEntry("nexl.source.queryString", "expression=%24%7Ball%7D");
	}

	@Test
	void shouldHaveCorrectOrder() {
		assertThat(provider.getOrder()).isEqualTo(1);
	}

	@Test
	void shouldImplementHttpRequestAwareInterface() {
		assertThat(provider).isInstanceOf(HttpRequestAwareConfigResourceProvider.class);
		assertThat(provider).isInstanceOf(ConfigResourceProvider.class);
	}

	@Test
	void shouldHandleFallbackMethodCall() {
		// Test the default interface method that calls the new method with null request
		String expectedUrl = "http://nexl:8181/test/profile.js";
		String mockResponse = """
                {
                    "fallback": true,
                    "method": "without-request"
                }
                """;

		when(restTemplate.getForObject(expectedUrl, String.class)).thenReturn(mockResponse);

		// When - Call the interface method without HttpServletRequest
		Map<String, Object> properties = ((ConfigResourceProvider) provider)
				.loadProperties("test", "profile", "nexl");

		// Then
		assertThat(properties).isNotEmpty();
		assertThat(properties).containsEntry("fallback", true);
		assertThat(properties).containsEntry("method", "without-request");
		assertThat(properties).containsEntry("nexl.source.application", "test");
		assertThat(properties).containsEntry("nexl.source.profile", "profile");
		// Should not contain request-specific metadata
		assertThat(properties).doesNotContainKey("nexl.source.requestUri");
	}
}