package com.edx.spring.config.central.server.admin;

import com.edx.spring.config.central.server.loader.ConfigResourceProvider;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

	private final ConfigProviderManager providerManager;
	private final List<ConfigResourceProvider> providers;

	/**
	 * Main admin dashboard page
	 */
	@GetMapping
	public String adminDashboard(Model model) {
		model.addAttribute("providers", getProviderStatus());
		model.addAttribute("primaryProvider", providerManager.getPrimaryProvider());
		return "admin/dashboard";
	}

	/**
	 * HTMX endpoint to get providers list
	 */
	@GetMapping("/providers")
	@HxRequest
	public String getProviders(Model model) {
		model.addAttribute("providers", getProviderStatus());
		return "admin/fragments/providers :: providers-list";
	}

	/**
	 * HTMX endpoint to toggle provider status
	 * In version 4.0.1, we use @HxTrigger for client-side events
	 */
	@PostMapping("/providers/{providerName}/toggle")
	@HxRequest
	@HxTrigger("providerToggled") // This sends a client-side event
	public String toggleProvider(@PathVariable String providerName, Model model) {
		log.info("Toggling provider: {}", providerName);

		boolean newState = providerManager.toggleProvider(providerName);

		model.addAttribute("providers", getProviderStatus());
		model.addAttribute("message", String.format("Provider %s is now %s",
				providerName, newState ? "enabled" : "disabled"));

		// Return the fragment to update
		return "admin/fragments/providers :: providers-list";
	}

	/**
	 * HTMX endpoint to change primary provider
	 */
	@PostMapping("/providers/{providerName}/primary")
	@HxRequest
	@HxTrigger("primaryProviderChanged")
	public String setPrimaryProvider(@PathVariable String providerName, Model model) {
		log.info("Setting primary provider to: {}", providerName);

		providerManager.setPrimaryProvider(providerName);

		model.addAttribute("providers", getProviderStatus());
		model.addAttribute("primaryProvider", providerName);
		model.addAttribute("message", String.format("Primary provider changed to %s", providerName));

		return "admin/fragments/providers :: providers-list";
	}

	/**
	 * HTMX endpoint to refresh git repositories info
	 */
	@GetMapping("/git-repos/refresh")
	@HxRequest
	public String refreshGitRepos(Model model) {
		model.addAttribute("gitRepos", getGitRepositories());
		return "admin/fragments/git-repos :: git-repos-list";
	}

	/**
	 * REST API endpoint for status (for external monitoring)
	 */
	@GetMapping("/api/status")
	@ResponseBody
	public Map<String, Object> getStatus() {
		Map<String, Object> status = new HashMap<>();
		status.put("providers", getProviderStatus());
		status.put("primaryProvider", providerManager.getPrimaryProvider());
		status.put("timestamp", System.currentTimeMillis());
		return status;
	}

	private List<Map<String, Object>> getProviderStatus() {
		return providers.stream().map(provider -> {
					String providerName = provider.getClass().getSimpleName();
					Map<String, Object> info = new HashMap<>();
					info.put("name", providerName);
					info.put("enabled", providerManager.isProviderEnabled(providerName));
					info.put("order", getProviderOrder(provider));
					info.put("isPrimary", providerName.toLowerCase().contains(providerManager.getPrimaryProvider().toLowerCase()));
					info.put("supportedLabels", getSupportedLabels(provider));
					return info;
				}).sorted((a, b) -> Integer.compare((Integer) a.get("order"), (Integer) b.get("order")))
				.toList();
	}

	private List<Map<String, Object>> getGitRepositories() {
		List<Map<String, Object>> repos = new ArrayList<>();

		providers.stream()
				.filter(provider -> provider.getClass().getSimpleName().toLowerCase().contains("git"))
				.forEach(provider -> {
					Map<String, Object> repoInfo = new HashMap<>();
					repoInfo.put("provider", provider.getClass().getSimpleName());
					repoInfo.put("url", "https://github.com/example/config");
					repoInfo.put("branch", "main");
					repoInfo.put("lastUpdate", "2025-01-01 12:00:00");
					repos.add(repoInfo);
				});

		if (repos.isEmpty()) {
			Map<String, Object> placeholder = new HashMap<>();
			placeholder.put("provider", "No Git providers configured");
			placeholder.put("url", "N/A");
			placeholder.put("branch", "N/A");
			placeholder.put("lastUpdate", "N/A");
			repos.add(placeholder);
		}

		return repos;
	}

	private int getProviderOrder(ConfigResourceProvider provider) {
		if (provider instanceof org.springframework.core.Ordered) {
			return provider.getOrder();
		}
		return 0;
	}

	private List<String> getSupportedLabels(ConfigResourceProvider provider) {
		return Arrays.asList("default", "production", "development");
	}
}
