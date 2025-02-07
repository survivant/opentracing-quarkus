import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.contrib.jaxrs2.client.ClientSpanDecorator;
import io.opentracing.contrib.jaxrs2.client.ClientTracingInterceptor;
import io.opentracing.contrib.jaxrs2.internal.URIUtils;
import io.opentracing.contrib.jaxrs2.serialization.InterceptorSpanDecorator;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

@Provider
public class TracingRestClientInitializer implements Feature {
    private static final Logger log = Logger.getLogger(TracingRestClientInitializer.class.getName());

    @Inject
    ObjectMapper mapper;

    private TracingRestClientInitializer.Builder builder;

    /**
     * When using this constructor application has to call {@link GlobalTracer#registerIfAbsent(Tracer)} to register
     * tracer instance.
     *
     * For a custom configuration use {@link TracingRestClientInitializer.Builder#build()}.
     */
    public TracingRestClientInitializer() {
        this(new TracingRestClientInitializer.Builder(GlobalTracer.get()));
    }

    private TracingRestClientInitializer(TracingRestClientInitializer.Builder builder) {
        this.builder = builder;
    }

    @Override
    public boolean configure(FeatureContext context) {
        if (log.isLoggable(Level.FINE)) {
            log.fine("Registering client OpenTracing, with configuration:" + builder.toString());
        }
        context.register(new CustomClientTracingFilter(builder.tracer, builder.spanDecorators),
                builder.priority);

        if (builder.traceSerialization) {
            context.register(
                    new ClientTracingInterceptor(builder.tracer, builder.serializationSpanDecorators),
                    builder.serializationPriority);
        }
        return true;
    }

    /**
     * Builder for configuring {@link Client} to trace outgoing requests.
     *
     * By default get's operation name is HTTP method and get is decorated with
     * {@link ClientSpanDecorator#STANDARD_TAGS} which adds set of standard tags.
     */
    public static class Builder {
        private Tracer tracer;
        private List<ClientSpanDecorator> spanDecorators;
        private List<InterceptorSpanDecorator> serializationSpanDecorators;
        private int priority;
        private int serializationPriority;
        private boolean traceSerialization;

        public Builder(Tracer tracer) {
            this.tracer = tracer;
            //this.spanDecorators = Collections.singletonList(ClientSpanDecorator.STANDARD_TAGS);
            this.spanDecorators = Collections.singletonList(new ClientSpanDecorator() {
                @Override
                public void decorateRequest(ClientRequestContext requestContext, Span span) {
                    Tags.COMPONENT.set(span, "jaxrs-TracingRestClientInitializer");
                    Tags.PEER_HOSTNAME.set(span, requestContext.getUri().getHost());
                    Tags.PEER_PORT.set(span, requestContext.getUri().getPort());

                    Tags.HTTP_METHOD.set(span, requestContext.getMethod());

                    var url = URIUtils.url(requestContext.getUri());
                    if (url != null) {
                        Tags.HTTP_URL.set(span, url);
                    }

                    span.setTag("http.headers", requestContext.getHeaders().toString());
                    span.setTag("http.request.body", util.LoggingFilter.getRequestBody(requestContext));
                }

                @Override
                public void decorateResponse(ClientResponseContext responseContext, Span span) {
                    span.setTag("http.response.body", util.LoggingFilter.getResponseBody(responseContext));
                    Tags.HTTP_STATUS.set(span, responseContext.getStatus());
                }
            });
            this.serializationSpanDecorators = Arrays.asList(InterceptorSpanDecorator.STANDARD_TAGS);
            // by default do not use Priorities.AUTHENTICATION due to security concerns
            this.priority = Priorities.HEADER_DECORATOR;
            this.serializationPriority = Priorities.ENTITY_CODER;
            this.traceSerialization = false;
        }

        /**
         * Set span decorators.
         * @return builder
         */
        public TracingRestClientInitializer.Builder withDecorators(List<ClientSpanDecorator> spanDecorators) {
            this.spanDecorators = spanDecorators;
            return this;
        }

        /**
         * Set serialization span decorators.
         * @return builder
         */
        public TracingRestClientInitializer.Builder withSerializationDecorators(List<InterceptorSpanDecorator> spanDecorators) {
            this.serializationSpanDecorators = spanDecorators;
            return this;
        }

        /**
         * @param priority the overriding priority for the registered component.
         *                 Default is {@link Priorities#HEADER_DECORATOR}
         * @return builder
         *
         * @see Priorities
         */
        public TracingRestClientInitializer.Builder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * @param serializationPriority the overriding priority for the registered component.
         *                              Default is {@link Priorities#ENTITY_CODER}
         * @return builder
         *
         * @see Priorities
         */
        public TracingRestClientInitializer.Builder withSerializationPriority(int serializationPriority) {
            this.serializationPriority = serializationPriority;
            return this;
        }

        /**
         * @param traceSerialization whether to trace serialization
         * @return builder
         */
        public TracingRestClientInitializer.Builder withTraceSerialization(boolean traceSerialization) {
            this.traceSerialization = traceSerialization;
            return this;
        }

        /**
         * @return client tracing feature. This feature should be manually registered to {@link Client}
         */
        public TracingRestClientInitializer build() {
            return new TracingRestClientInitializer(this);
        }
    }

}


