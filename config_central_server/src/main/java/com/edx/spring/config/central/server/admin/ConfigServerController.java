package com.edx.spring.config.central.server.admin;

import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Config Server Admin", description = "Administrative endpoints for the Config Server")
public class ConfigServerController {

	private final List<ConfigResourceProvider> providers;
	private final EnvironmentRepository environmentRepository;

	@GetMapping("/providers")
	@Operation(summary = "Get available config providers",
			description = "Returns information about all registered configuration providers")
	public List<Map<String, Object>> getProviders() {
		return providers.stream()
				.map(provider -> {
					Map<String, Object> providerInfo = new HashMap<>();
					providerInfo.put("name", provider.getClass().getSimpleName());
					providerInfo.put("order", provider.getOrder());
					providerInfo.put("type", provider.getClass().getName());
					return providerInfo;
				})
				.collect(Collectors.toList());
	}

	@GetMapping("/config/{application}/{profile}")
	@Operation(summary = "Get configuration for application and profile",
			description = "Retrieves configuration using the default label")
	public Environment getConfig(
			@Parameter(description = "Application name") @PathVariable String application,
			@Parameter(description = "Profile name") @PathVariable String profile) {
		return environmentRepository.findOne(application, profile, null);
	}

	@GetMapping("/config/{application}/{profile}/{label}")
	@Operation(summary = "Get configuration for application, profile and label",
			description = "Retrieves configuration for specific application, profile and label")
	public Environment getConfigWithLabel(
			@Parameter(description = "Application name") @PathVariable String application,
			@Parameter(description = "Profile name") @PathVariable String profile,
			@Parameter(description = "Label/branch name") @PathVariable String label) {
		return environmentRepository.findOne(application, profile, label);
	}

	@GetMapping("/health")
	@Operation(summary = "Config server health check",
			description = "Returns the health status of the config server and its providers")
	public Map<String, Object> health() {
		Map<String, Object> health = new HashMap<>();
		health.put("status", "UP");
		health.put("providers", providers.size());
		health.put("timestamp", System.currentTimeMillis());
		return health;
	}
}