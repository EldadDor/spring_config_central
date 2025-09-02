package com.edx.spring.config.central.server

import org.springframework.stereotype.Component
import java.net.ProxySelector
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

@Component
class KNexlService(private val httpClient: HttpClient = createDefaultHttpClient()) {

	companion object {
		private fun createDefaultHttpClient(): HttpClient {
			return HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.version(HttpClient.Version.HTTP_1_1)
				.build()
		}
	}

	fun callNexlServerForJava(path: String, expression: String): NexlResult {
		return try {
			val result = callNexlServer(path, expression)
			if (result.isSuccess) {
				NexlResult.success(result.getOrNull())
			} else {
				NexlResult.failure(result.exceptionOrNull())
			}
		} catch (e: Exception) {
			NexlResult.failure(e)
		}
	}

	fun callNexlServer(path: String, expression: String): Result<String> {
		return try {
			val baseUrl = "http://nexl:8181"
			// Clean the path - remove leading slash if present since we'll add it
			val cleanPath = if (path.startsWith("/")) path else "/$path"

			// Build URL with or without expression parameter
			val fullUrl = if (expression.isNotEmpty()) {
				val encodedExpression = URLEncoder.encode(expression, StandardCharsets.UTF_8)
				"$baseUrl$cleanPath?expression=$encodedExpression"
			} else {
				// If no expression, just use the path as-is (it might already contain query parameters)
				"$baseUrl$cleanPath"
			}

			println("Target URL: $fullUrl")

			// Check proxy settings
			val uri = URI.create(fullUrl)
			val proxies = ProxySelector.getDefault().select(uri)

			val request = HttpRequest.newBuilder()
				.uri(URI.create(fullUrl))
				.header("User-Agent", "spring-config-central/1.0")
				.header("Accept", "*/*")
				.timeout(Duration.ofSeconds(30))
				.GET()
				.build()

			val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

			when (response.statusCode()) {
				200 -> Result.success(response.body())
				else -> Result.failure(RuntimeException("HTTP Error: ${response.statusCode()} - ${response.body()}"))
			}
		} catch (e: Exception) {
			println("Error calling NEXL server: ${e.message}")
			println("Exception type: ${e.javaClass.simpleName}")
			e.printStackTrace()
			Result.failure(e)
		}
	}


	data class NexlResult(
		val isSuccess: Boolean,
		val data: String?,
		val exception: Throwable?
	) {
		companion object {
			fun success(data: String?): NexlResult = NexlResult(true, data, null)
			fun failure(exception: Throwable?): NexlResult = NexlResult(false, null, exception)
		}
	}

	fun callNexlServerAsync(path: String, expression: String, callback: (Result<String>) -> Unit) {
		Thread {
			val result = callNexlServer(path, expression)
			callback(result)
		}.start()
	}

}