package com.example.opentracing.quarkus;

import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.util.List;


import com.example.opentracing.TracingConstant;
import com.example.opentracing.quarkus.util.LoggingFilter;
import io.opentracing.Span;
import io.opentracing.contrib.jaxrs2.client.ClientSpanDecorator;
import io.opentracing.util.GlobalTracer;

@Provider
public class TracingRestClientInitializer implements Feature {

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new CustomClientTracingFilter(
                        GlobalTracer.get(),
                        List.of(ClientSpanDecorator.STANDARD_TAGS, new ClientSpanDecorator() {
                            @Override
                            public void decorateRequest(ClientRequestContext requestContext, Span span) {
                                span.setTag(TracingConstant.TRACING_HTTP_HEADERS, requestContext.getHeaders().toString());
                                span.setTag(TracingConstant.TRACING_HTTP_REQUEST_BODY, LoggingFilter.getRequestBody(requestContext));
                            }

                            @Override
                            public void decorateResponse(ClientResponseContext responseContext, Span span) {
                                span.setTag(TracingConstant.TRACING_HTTP_RESPONSE_BODY, LoggingFilter.getResponseBody(responseContext));
                            }
                        })),
                Priorities.HEADER_DECORATOR+10);
        return true;
    }
}


