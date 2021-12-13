package com.example.opentracing.spring.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import com.example.opentracing.TracingConstant;
import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * The type Body extractor dispatcher servlet.
 */
@Configuration
public class BodyExtractorDispatcherServlet extends DispatcherServlet {
    private final static Logger LOGGER = LoggerFactory.getLogger(BodyExtractorDispatcherServlet.class);

    /**
     * The constant MAX_PAYLOAD_LENGTH_TO_LOG.
     */
    final static int MAX_PAYLOAD_LENGTH_TO_LOG = 5120;

    /**
     * The Tracer.
     */
    @Autowired
    Tracer tracer;

    /**
     * The Tracing servlet filter span decorator.
     */
    @Autowired
    TracingServletFilterSpanDecorator tracingServletFilterSpanDecorator;

    /**
     * Will encapsulate the request and response to be able to read the body more than once
     * @param request request
     * @param response response
     * @throws Exception is there is a error
     */
    @Override
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!(request instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper(request);
        }
        if (!(response instanceof ContentCachingResponseWrapper)) {
            response = new ContentCachingResponseWrapper(response);
        }

        HandlerExecutionChain handler = getHandler(request);

        try {
            super.doDispatch(request, response);
        } finally {
            extractPayload(request, response, handler);
            updateResponse(response);
        }
    }

    /**
     * Extract the body of the request and response and add it into Jaeger tracing
     * @param requestToCache request
     * @param responseToCache response
     * @param handler handler
     */
    private void extractPayload(HttpServletRequest requestToCache, HttpServletResponse responseToCache, HandlerExecutionChain handler) {
        String requestBody = getRequestPayload(requestToCache);
        String responseBody = getResponsePayload(responseToCache);

        Span span = tracer.activeSpan();
        if(span==null) { // span closed
            String uuid = (String)requestToCache.getAttribute(TracingConstant.JAEGER_PROPAGATION_KEY);

            span = tracingServletFilterSpanDecorator.getSpanMap().get(TracingConstant.SPAN_REQUEST_PREFIX+ uuid);

            if(span!=null) {
                span.setTag(TracingConstant.TRACING_HTTP_REQUEST_BODY, requestBody);
                span.setTag(TracingConstant.TRACING_HTTP_RESPONSE_BODY, responseBody);
            }
        } else {
            span.setTag(TracingConstant.TRACING_HTTP_REQUEST_BODY, requestBody);
            span.setTag(TracingConstant.TRACING_HTTP_RESPONSE_BODY, responseBody);
        }

        LOGGER.debug("Request URI [{}] body [{}]", requestToCache.getRequestURI(), requestBody);
        LOGGER.debug("Response URI [{}] body [{}] ",requestToCache.getRequestURI(), responseBody);
    }


    private static String getRequestPayload(HttpServletRequest request) {
        ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        return wrapper != null
                ? extractPayloadFromBuffer(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding())
                : null;
    }

    private static String extractPayloadFromBuffer(byte[] buf, String characterEncoding) {
        if (buf.length > 0) {
            int length = Math.min(buf.length, MAX_PAYLOAD_LENGTH_TO_LOG);
            try {
                return new String(buf, 0, length, characterEncoding);
            } catch (UnsupportedEncodingException ex) {
                return "[Error extracting payload]";
            }
        }
        return "";
    }

    private static String getResponsePayload(HttpServletResponse response) {
        ContentCachingResponseWrapper wrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        if (wrapper != null) {

            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                int length = Math.min(buf.length, MAX_PAYLOAD_LENGTH_TO_LOG);
                try {
                    return new String(buf, 0, length, wrapper.getCharacterEncoding());
                }
                catch (UnsupportedEncodingException ex) {
                    // NOOP
                }
            }
        }
        return ""; // empty
    }

    private static void updateResponse(HttpServletResponse response) throws IOException {
        ContentCachingResponseWrapper responseWrapper = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        responseWrapper.copyBodyToResponse();
    }

}
