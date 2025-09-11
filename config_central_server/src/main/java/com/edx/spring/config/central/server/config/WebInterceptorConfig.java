
package com.edx.spring.config.central.server.config;

import com.edx.spring.config.central.server.service.ConfigResponseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Configuration
public class WebInterceptorConfig implements WebMvcConfigurer {

	@Autowired
	private ApplicationContext applicationContext; ;
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new ConfigResponseInterceptor(applicationContext))
				.addPathPatterns("/**")
				.excludePathPatterns(
						"/actuator/**",
						"/error",
						"/admin/**",
						"/css/**",      // Add these
						"/fonts/**",    // Add these
						"/js/**",       // Add these
						"/static/**"    // Add these
				);
	}

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer
				.favorParameter(false)
				.ignoreAcceptHeader(false)
				.defaultContentType(MediaType.APPLICATION_JSON)
				.mediaType("css", MediaType.valueOf("text/css"))
				.mediaType("js", MediaType.valueOf("application/javascript"))
				.mediaType("woff", MediaType.valueOf("font/woff"))
				.mediaType("woff2", MediaType.valueOf("font/woff2"));
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// Handle CSS files
		registry.addResourceHandler("/css/**")
				.addResourceLocations("classpath:/static/css/")
				.setCachePeriod(3600);

		// Handle font files
		registry.addResourceHandler("/fonts/**")
				.addResourceLocations("classpath:/static/fonts/")
				.setCachePeriod(3600);

		// Handle static resources with /static prefix (if needed)
		registry.addResourceHandler("/static/**")
				.addResourceLocations("classpath:/static/")
				.setCachePeriod(3600);
	}

	@Bean
	public FilterRegistrationBean<ContentCachingFilter> contentCachingFilter() {
		FilterRegistrationBean<ContentCachingFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(new ContentCachingFilter());
		registrationBean.addUrlPatterns("/*");
		registrationBean.setOrder(1);
		return registrationBean;
	}

	public static class ContentCachingFilter implements Filter {
		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

			if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
				HttpServletRequest httpRequest = (HttpServletRequest) request;
				HttpServletResponse httpResponse = (HttpServletResponse) response;

				// Skip caching for Swagger and admin endpoints
				if (isNexlEndpoint(httpRequest) && !isSwaggerOrAdminEndpoint(httpRequest)) {
					ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(httpResponse);
					chain.doFilter(request, wrapper);
					return;
				}
			}

			chain.doFilter(request, response);
		}

		private boolean isNexlEndpoint(HttpServletRequest request) {
			String uri = request.getRequestURI();
			return uri != null && uri.contains("/nexl");
		}

		private boolean isSwaggerOrAdminEndpoint(HttpServletRequest request) {
			String uri = request.getRequestURI();
			return uri != null && (
					uri.startsWith("/swagger-ui") ||
							uri.startsWith("/v3/api-docs") ||
							uri.startsWith("/admin") ||
							uri.startsWith("/webjars") ||
							uri.startsWith("/swagger-resources")
			);
		}
	}
}