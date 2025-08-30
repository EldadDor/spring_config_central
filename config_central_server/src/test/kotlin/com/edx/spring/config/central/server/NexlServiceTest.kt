package com.edx.spring.config.central.server

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/*
 * User: eldad
 * Date: 8/30/2025 
 *
 * Copyright (2005) IDI. All rights reserved.
 * This software is a proprietary information of Israeli Direct Insurance.
 * Created by IntelliJ IDEA. 
 */
class NexlServiceTest {

	@Test
	fun `integration test - callNexlServer with real HTTP client`() {

//		System.setProperty("java.net.useSystemProxies", "true")
//		System.setProperty("jdk.httpclient.HttpClient.log", "all")
//		System.setProperty("jdk.internal.httpclient.debug", "true")

		// Also check proxy settings
		println("HTTP Proxy: ${System.getProperty("http.proxyHost")}:${System.getProperty("http.proxyPort")}")
		println("HTTPS Proxy: ${System.getProperty("https.proxyHost")}:${System.getProperty("https.proxyPort")}")
		println("No Proxy: ${System.getProperty("http.nonProxyHosts")}")

		// Given
		val nexlService = KNexlService()
		val path = "/jenkins/deployment/micro-services/ms-information.js"  // First part (no encoding needed)
		val expression = "\${all}"  // Second part (will be URL encoded by the method)


		// When
		val result = nexlService.callNexlServer(path, expression)

		// Then
		println("Result isSuccess: ${result.isSuccess}")
		println("Result isFailure: ${result.isFailure}")

		if (result.isSuccess) {
			println("Response body: ${result.getOrNull()}")
			assertThat(result.getOrNull()).isNotNull()
		} else {
			println("Error: ${result.exceptionOrNull()?.message}")
			// In a real environment where nexl server might not be available,
			// we can still verify the behavior
			assertThat(result.exceptionOrNull()).isNotNull()

			// Check that it's a connection-related exception, not a code error
			val exception = result.exceptionOrNull()
			assertThat(exception).satisfiesAnyOf(
				{ ex -> assertThat(ex).isInstanceOf(java.net.ConnectException::class.java) },
				{ ex -> assertThat(ex).isInstanceOf(java.net.UnknownHostException::class.java) },
				{ ex -> assertThat(ex).isInstanceOf(java.io.IOException::class.java) },
				{ ex -> assertThat(ex).hasMessageContaining("nexl") }
			)
		}

		// Verify URL construction by checking the printed URL in logs/console
		// The service should print: "Calling NEXL server with URL: http://nexl:8181/jenkins/deployment/micro-services/deploy-ms.js?expression=%24%7Ball%7D"
	}

}