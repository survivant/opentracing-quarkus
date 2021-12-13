package com.example.opentracing.spring.webclient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.client.TracingExchangeFilterFunction;
import io.opentracing.contrib.spring.web.client.WebClientSpanDecorator;

@Component
public class LoggingTracingWebClientFilterFunction extends TracingExchangeFilterFunction {
    final static Logger LOGGER = LoggerFactory.getLogger(LoggingTracingWebClientFilterFunction.class);

    private Map<String,Span> spanMap;

    private Tracer tracer;
    private TracingWebClientDecorators decorator;

    @Autowired
    public LoggingTracingWebClientFilterFunction(Tracer tracer, TracingWebClientDecorators decorator) {
        super(tracer, List.of(new WebClientSpanDecorator.StandardTags(), decorator));
        this.tracer = tracer;
        this.decorator = decorator;

        spanMap = new HashMap<>();
        decorator.setSpanMap(spanMap);
    }

    public Map<String, Span> getSpanMap() {
        return spanMap;
    }

    public Tracer getTracer() {
        return tracer;
    }

    public void setTracer(Tracer tracer) {
        this.tracer = tracer;
    }
}
