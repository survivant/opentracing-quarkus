package com.example.opentracing.spring.resttemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import com.example.opentracing.TracingConstant;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.RestTemplateSpanDecorator;
import io.opentracing.contrib.spring.web.client.TracingRestTemplateInterceptor;

@Component
public class LoggingTracingRestTemplateInterceptor extends TracingRestTemplateInterceptor {
    private static Map<String,String>  spanMap = new HashMap<>();

    private Tracer tracer;

    public LoggingTracingRestTemplateInterceptor(Tracer tracer) {
        super(tracer, List.of(new RestTemplateSpanDecorator.StandardTags(),
                new RestTemplateSpanDecorator() {

                    @Override
                    public void onRequest(HttpRequest httpRequest, Span span) {
                        span.setTag(TracingConstant.TRACING_HTTP_HEADERS, httpRequest.getHeaders().toString());
                        span.setTag(TracingConstant.TRACING_HTTP_REQUEST_BODY, spanMap.get(span.context().toTraceId()));
                    }

                    @Override
                    public void onResponse(HttpRequest httpRequest, ClientHttpResponse clientHttpResponse, Span span) {
                        span.setTag(TracingConstant.TRACING_HTTP_HEADERS, httpRequest.getHeaders().toString());

                        try {
                            InputStreamReader isr = new InputStreamReader(clientHttpResponse.getBody(), StandardCharsets.UTF_8);
                            String bodyString = new BufferedReader(isr).lines().collect(Collectors.joining("\n"));
                            span.setTag(TracingConstant.TRACING_HTTP_RESPONSE_BODY, bodyString);
                        } catch (Exception ignore){

                        }

                    }

                    @Override
                    public void onError(HttpRequest httpRequest, Throwable throwable, Span span) {
                        span.setTag("RestTemplate onError", throwable.toString());
                    }
                }));

        this.tracer = tracer;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        String traceid = tracer.activeSpan().context().toTraceId();
        spanMap.put(traceid, new String(body, StandardCharsets.UTF_8));

        ClientHttpResponse response = super.intercept(httpRequest, body, execution);

        spanMap.remove(traceid);

        return response;
    }
}
