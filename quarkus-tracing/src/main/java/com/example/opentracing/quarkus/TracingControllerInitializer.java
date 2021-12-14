package com.example.opentracing.quarkus;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.util.Collections;


import com.example.opentracing.TracingConstant;
import com.example.opentracing.quarkus.util.LoggingFilter;
import io.opentracing.Span;
import io.opentracing.contrib.jaxrs2.server.ServerSpanDecorator;
import io.opentracing.util.GlobalTracer;

@Provider
public class TracingControllerInitializer implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        context.register(new CustomServerTracingFilter(
                        GlobalTracer.get(),
                        Collections.singletonList(new ServerSpanDecorator() {
                            @Override
                            public void decorateRequest(ContainerRequestContext requestContext, Span span) {
                                span.setTag(TracingConstant.TRACING_HTTP_HEADERS, requestContext.getHeaders().toString());
                                span.setTag(TracingConstant.TRACING_HTTP_REQUEST_BODY, LoggingFilter.getRequestBody(requestContext));
                            }

                            @Override
                            public void decorateResponse(ContainerResponseContext responseContext, Span span) {
                                span.setTag(TracingConstant.TRACING_HTTP_RESPONSE_BODY, LoggingFilter.getResponseBody(responseContext));
                            }
                        })),
                Priorities.HEADER_DECORATOR+10);
    }
}


