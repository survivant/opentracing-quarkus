package com.example.opentracing.quarkus;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;
import java.util.List;


import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.server.ServerSpanDecorator;

public class CustomServerTracingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private final Tracer tracer;
    private final List<ServerSpanDecorator> serverSpanDecorators;

    public CustomServerTracingFilter(Tracer tracer, List<ServerSpanDecorator> serverSpanDecorators) {
        this.tracer = tracer;
        this.serverSpanDecorators = serverSpanDecorators;
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        if (serverSpanDecorators != null) {
            Span span = tracer.activeSpan();
            for (ServerSpanDecorator decorator: serverSpanDecorators) {
                decorator.decorateRequest(containerRequestContext, span);
            }
        }

    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        if (serverSpanDecorators != null) {
            Span span = tracer.activeSpan();
            for (ServerSpanDecorator decorator: serverSpanDecorators) {
                decorator.decorateResponse(containerResponseContext, span);
            }
        }
    }
}
