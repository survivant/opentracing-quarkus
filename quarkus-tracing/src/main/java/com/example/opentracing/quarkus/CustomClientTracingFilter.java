package com.example.opentracing.quarkus;

import static io.opentracing.contrib.jaxrs2.internal.SpanWrapper.PROPERTY_NAME;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


import org.eclipse.microprofile.opentracing.Traced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.client.ClientHeadersInjectTextMap;
import io.opentracing.contrib.jaxrs2.client.ClientSpanDecorator;
import io.opentracing.contrib.jaxrs2.client.TracingProperties;
import io.opentracing.contrib.jaxrs2.internal.CastUtils;
import io.opentracing.contrib.jaxrs2.internal.SpanWrapper;
import io.opentracing.noop.NoopScopeManager.NoopScope;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

/**
 * @author Pavol Loffay
 */
@Priority(Priorities.HEADER_DECORATOR+10)
public class CustomClientTracingFilter implements ClientRequestFilter, ClientResponseFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomClientTracingFilter.class);

    private Tracer tracer;
    private List<ClientSpanDecorator> spanDecorators;

    public CustomClientTracingFilter(Tracer tracer, List<ClientSpanDecorator> spanDecorators) {
        this.tracer = tracer;
        this.spanDecorators = new ArrayList<>(spanDecorators);
    }

    @Override
    public void filter(ClientRequestContext requestContext) {
        if (tracingDisabled(requestContext)) {
            LOGGER.trace("Client tracing disabled");
            return;
        }

        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(requestContext.getMethod())
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

        SpanContext parentSpanContext = CastUtils.cast(requestContext.getProperty(TracingProperties.CHILD_OF),
                SpanContext.class);
        if (parentSpanContext != null) {
            spanBuilder.ignoreActiveSpan()
                    .asChildOf(parentSpanContext);
        }

        /**
         * Do not create Scope - there is no way to deactivate it on UnknownHostException
         */
        final Span span = spanBuilder.start();

        if (spanDecorators != null) {
            for (ClientSpanDecorator decorator: spanDecorators) {
                decorator.decorateRequest(requestContext, span);
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting client span");
        }

        tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new ClientHeadersInjectTextMap(requestContext.getHeaders()));
        requestContext.setProperty(PROPERTY_NAME, new SpanWrapper(span, new NoopScope() {
            @Override
            public void close() {
            }
        }));
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        SpanWrapper spanWrapper = CastUtils
                .cast(requestContext.getProperty(PROPERTY_NAME), SpanWrapper.class);
        if (spanWrapper != null && !spanWrapper.isFinished()) {
            LOGGER.trace("Finishing client span");

            if (spanDecorators != null) {
                for (ClientSpanDecorator decorator: spanDecorators) {
                    decorator.decorateResponse(responseContext, spanWrapper.get());
                }
            }

            spanWrapper.finish();
        }
    }

    private boolean tracingDisabled(ClientRequestContext clientRequestContext) {
        Boolean tracingDisabled = CastUtils.cast(clientRequestContext.getProperty(TracingProperties.TRACING_DISABLED), Boolean.class);
        if (tracingDisabled != null && tracingDisabled) {
            return true;
        }

        Object invokedMethod = clientRequestContext.getProperty("org.eclipse.microprofile.rest.client.invokedMethod");
        if (invokedMethod == null) {
            return false;
        }

        Method method = (Method) invokedMethod;
        Traced traced = method.getAnnotation(Traced.class);
        if (traced != null && !traced.value()) {
            return true;
        }
        return false;
    }
}
