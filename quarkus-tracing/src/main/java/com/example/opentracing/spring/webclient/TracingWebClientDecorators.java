package com.example.opentracing.spring.webclient;

import java.util.Map;
import java.util.Objects;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

import com.example.opentracing.TracingConstant;
import io.opentracing.Span;
import io.opentracing.contrib.spring.web.client.WebClientSpanDecorator;


public class TracingWebClientDecorators implements WebClientSpanDecorator {
    final static Logger LOGGER = LoggerFactory.getLogger(TracingWebClientDecorators.class);

    private Map<String,Span> spanMap;

    public void setSpanMap(Map<String, Span> spanMap) {
        this.spanMap = spanMap;
    }

    @Override
    public void onRequest(ClientRequest clientRequest, Span span) {
        spanMap.put(TracingConstant.SPAN_REQUEST_PREFIX + Objects.requireNonNull(clientRequest.headers().get(TracingConstant.JAEGER_PROPAGATION_KEY)).get(0), span);
    }

    @Override
    public void onResponse(ClientRequest clientRequest, ClientResponse clientResponse, Span span) {
        spanMap.put(TracingConstant.SPAN_RESPONSE_PREFIX + Objects.requireNonNull(clientRequest.headers().get(TracingConstant.JAEGER_PROPAGATION_KEY)).get(0), span);
    }

    @Override
    public void onError(ClientRequest clientRequest, Throwable throwable, Span span) {
        span.setTag("WebClient onError", clientRequest.toString());
    }

    @Override
    public void onCancel(ClientRequest clientRequest, Span span) {
        span.setTag("WebClient onCancel", clientRequest.toString());
    }

}
