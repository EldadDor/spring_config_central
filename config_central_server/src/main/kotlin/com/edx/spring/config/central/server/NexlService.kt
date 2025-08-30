package com.edx.spring.config.central.server

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

class NexlService {

	private val httpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(10))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build()

	fun callNexlServer(expression: String): Result<String> {
		return try {
			val baseUrl = "http://nexl:8181/jenkins/deployment/micro-services/deploy-ms.js"
			val encodedExpression = URLEncoder.encode(expression, StandardCharsets.UTF_8)
			val fullUrl = "$baseUrl?expression=$encodedExpression"

			println("Calling NEXL server with URL: $fullUrl")

			val request = HttpRequest.newBuilder()
				.uri(URI.create(fullUrl))
				.header("Accept", "application/json")
				.header("User-Agent", "NexlService/1.0")
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
			Result.failure(e)
		}
	}

	fun callNexlServerAsync(expression: String, callback: (Result<String>) -> Unit) {
		Thread {
			val result = callNexlServer(expression)
			callback(result)
		}.start()
	}
}
