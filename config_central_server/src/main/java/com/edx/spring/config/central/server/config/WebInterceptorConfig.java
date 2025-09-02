package com.edx.spring.config.central.server.config;

import com.edx.spring.config.central.server.service.ConfigResponseInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class WebInterceptorConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new ConfigResponseInterceptor())
				.addPathPatterns("/**")
				.excludePathPatterns("/actuator/**", "/error");
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
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {

			if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
				HttpServletRequest httpRequest = (HttpServletRequest) request;
				HttpServletResponse httpResponse = (HttpServletResponse) response;

				if (isNexlEndpoint(httpRequest)) {
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
	}
}
