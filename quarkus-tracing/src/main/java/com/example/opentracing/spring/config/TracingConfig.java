package com.example.opentracing.spring.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.opentracing.spring.resttemplate.LoggingTracingRestTemplateInterceptor;
import com.example.opentracing.spring.resttemplate.RestTemplateLoggingInterceptor;
import com.example.opentracing.spring.servlet.BodyExtractorDispatcherServlet;
import com.example.opentracing.spring.servlet.TracingServletFilterSpanDecorator;
import com.example.opentracing.spring.webclient.TracingWebClientDecorators;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.interceptor.HandlerInterceptorSpanDecorator;
import io.opentracing.contrib.spring.web.interceptor.TracingHandlerInterceptor;
import io.opentracing.contrib.spring.web.webfilter.TracingWebFilter;
import io.opentracing.contrib.spring.web.webfilter.WebFluxSpanDecorator;
import io.opentracing.contrib.web.servlet.filter.ServletFilterSpanDecorator;
import io.opentracing.contrib.web.servlet.filter.TracingFilter;


@Configuration
public class TracingConfig {
	@Autowired
	private Tracer tracer;

	@Autowired
	private TracingServletFilterSpanDecorator tracingServletFilterSpanDecorator;

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		LoggingTracingRestTemplateInterceptor tracingInterceptor = new LoggingTracingRestTemplateInterceptor(tracer);

		ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
		RestTemplate restTemplate = new RestTemplate(factory);

		// if we want to log request body
		restTemplate.setInterceptors(List.of(new RestTemplateLoggingInterceptor(), tracingInterceptor));
		return restTemplate;
	}

	@Bean
	public TracingWebClientDecorators decorators(){
		return new TracingWebClientDecorators();
	}

	/**
	 * Use span decorator to add a correlation id span tag
	 *
	 * Filter order configured to run after Tracer Filter
	 *
	 * @return span decorators
	 */
	@Bean
	public List<ServletFilterSpanDecorator> spanDecorator(){
		List<ServletFilterSpanDecorator> decorators = new ArrayList<>();
		decorators.add(ServletFilterSpanDecorator.STANDARD_TAGS);
		decorators.add(tracingServletFilterSpanDecorator);

		return decorators;
	}

	// MONO
	@Bean
	public TracingWebFilter tracingWebFilter(Tracer tracer) {
		return new TracingWebFilter(tracer, Integer.MIN_VALUE, Pattern.compile(""), Collections.emptyList(),
				Arrays.asList(new WebFluxSpanDecorator.StandardTags(), new WebFluxSpanDecorator.WebFluxTags(), new WebFluxSpanDecorator() {
					@Override
					public void onRequest(ServerWebExchange exchange, Span span) {
						span.setTag("TracingWebFilter onRequest", exchange.toString());
					}

					@Override
					public void onResponse(ServerWebExchange exchange, Span span) {
						span.setTag("TracingWebFilter onResponse",exchange.toString());
					}

					@Override
					public void onError(ServerWebExchange exchange, Throwable exception, Span span) {
						span.setTag("TracingWebFilter onError", exception.toString());
					}
				}));
	}

	// SERVLET
	@Bean
	public TracingFilter tracingFilter(Tracer tracer, List<ServletFilterSpanDecorator> spanDecorators, Pattern skipPattern){
		List<ServletFilterSpanDecorator> decorators = new ArrayList<>();
		decorators.add(ServletFilterSpanDecorator.STANDARD_TAGS);
		decorators.add(tracingServletFilterSpanDecorator);

		return new TracingFilter(tracer,decorators,skipPattern);
	}

	// SERVLET
	@Bean
	public WebMvcConfigurer tracingHandlerInterceptor(final Tracer tracer) {
		return new WebMvcConfigurer() {
			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				List<HandlerInterceptorSpanDecorator> decorators = Arrays.asList(HandlerInterceptorSpanDecorator.STANDARD_LOGS,
							HandlerInterceptorSpanDecorator.HANDLER_METHOD_OPERATION_NAME);

				registry.addInterceptor(new TracingHandlerInterceptor(tracer, decorators));
			}
		};
	}

	// SERVLET
	@Bean
	public ServletRegistrationBean dispatcherRegistration() {
		return new ServletRegistrationBean(dispatcherServlet());
	}

	@Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
	public DispatcherServlet dispatcherServlet() {
		return new BodyExtractorDispatcherServlet();
	}


}