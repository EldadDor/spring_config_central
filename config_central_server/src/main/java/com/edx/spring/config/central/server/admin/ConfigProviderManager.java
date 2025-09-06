package com.edx.spring.config.central.server.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ConfigProviderManager {

	private final Map<String, Boolean> providerStates = new ConcurrentHashMap<>();
	private final ApplicationEventPublisher eventPublisher;

	@Value("${config.providers.primary:nexl}")
	private String primaryProvider;

	public ConfigProviderManager(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	public boolean isProviderEnabled(String providerName) {
		return providerStates.getOrDefault(providerName, true);
	}

	public boolean toggleProvider(String providerName) {
		boolean newState = !isProviderEnabled(providerName);
		providerStates.put(providerName, newState);
		log.info("Provider {} is now {}", providerName, newState ? "enabled" : "disabled");

		// Publish event for other components to react
		eventPublisher.publishEvent(new ProviderStateChangeEvent(providerName, newState));
		return newState;
	}

	public void setPrimaryProvider(String providerName) {
		this.primaryProvider = providerName;
		log.info("Primary provider changed to: {}", providerName);
		eventPublisher.publishEvent(new PrimaryProviderChangeEvent(providerName));
	}

	public String getPrimaryProvider() {
		return primaryProvider;
	}

	// Event classes
	public static class ProviderStateChangeEvent {
		public final String providerName;
		public final boolean enabled;

		public ProviderStateChangeEvent(String providerName, boolean enabled) {
			this.providerName = providerName;
			this.enabled = enabled;
		}
	}

	public static class PrimaryProviderChangeEvent {
		public final String providerName;

		public PrimaryProviderChangeEvent(String providerName) {
			this.providerName = providerName;
		}
	}
}
