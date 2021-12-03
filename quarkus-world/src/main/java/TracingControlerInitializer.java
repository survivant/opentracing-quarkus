import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import java.util.Collections;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Span;
import io.opentracing.contrib.jaxrs2.internal.URIUtils;
import io.opentracing.contrib.jaxrs2.server.OperationNameProvider;
import io.opentracing.contrib.jaxrs2.server.ServerSpanDecorator;
import io.opentracing.contrib.jaxrs2.server.ServerTracingDynamicFeature;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

@Provider
public class TracingControlerInitializer implements DynamicFeature {

    @Inject
    ObjectMapper mapper;

    private final ServerTracingDynamicFeature serverTracingDynamicFeature =
            new ServerTracingDynamicFeature.Builder(GlobalTracer.get())
                    .withOperationNameProvider(OperationNameProvider.ClassNameOperationName.newBuilder())
                    .withDecorators(Collections.singletonList(new ServerSpanDecorator() {
                        @Override
                        public void decorateRequest(ContainerRequestContext requestContext, Span span) {
                            Tags.COMPONENT.set(span, "jaxrs-TracingControlerInitializer");
                            Tags.HTTP_METHOD.set(span, requestContext.getMethod());

                            var url = URIUtils.url(requestContext.getUriInfo().getRequestUri());
                            if (url != null) {
                                Tags.HTTP_URL.set(span, url);
                            }

                            span.setTag("http.headers", requestContext.getHeaders().toString());
                            span.setTag("http.request.body", util.LoggingFilter.getRequestBody(requestContext));
                        }

                        @Override
                        public void decorateResponse(ContainerResponseContext responseContext, Span span) {
                            span.setTag("http.response.body", util.LoggingFilter.getResponseBody(responseContext));
                            Tags.HTTP_STATUS.set(span, responseContext.getStatus());
                        }
                    }))
                    .withTraceSerialization(false)
                    .build();

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        serverTracingDynamicFeature.configure(resourceInfo, context);
    }

}