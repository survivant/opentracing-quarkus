package com.example.opentracing.spring.webclient;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;


import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.example.opentracing.TracingConstant;
import io.opentracing.Span;

public class WebClientBodyExtractor {
    final static Logger LOGGER = LoggerFactory.getLogger(WebClientBodyExtractor.class);

    private LoggingTracingWebClientFilterFunction filter;
    private String requestBody;
    private String responseBody;

    public Request init(Request request, final LoggingTracingWebClientFilterFunction filter) {
        this.filter = filter;

        /**
         * new request
         */
        request.onRequestContent((theRequest, content) -> {
            Span span = filter.getSpanMap().get(TracingConstant.SPAN_REQUEST_PREFIX + request.getHeaders().get(TracingConstant.JAEGER_PROPAGATION_KEY));

            String requestBody = toString(content, getCharset(theRequest.getHeaders()));
            span.setTag(TracingConstant.TRACING_HTTP_REQUEST_BODY, requestBody);
        });
        /**
         * request completed
         */
        request.onRequestSuccess(theRequest -> {
            if(StringUtils.isEmpty(requestBody)){
                Span span = filter.getSpanMap().get(TracingConstant.SPAN_REQUEST_PREFIX + request.getHeaders().get(TracingConstant.JAEGER_PROPAGATION_KEY));
                span.setTag(TracingConstant.TRACING_HTTP_REQUEST_BODY, "");
            }
        });
        /**
         * new response
         */
        request.onResponseContent((theResponse, content) -> {
            Span span = filter.getSpanMap().get(TracingConstant.SPAN_RESPONSE_PREFIX + theResponse.getRequest().getHeaders().get(TracingConstant.JAEGER_PROPAGATION_KEY));

            responseBody = toString(content, getCharset(theResponse.getHeaders()));

            span.setTag(TracingConstant.TRACING_HTTP_RESPONSE_BODY, responseBody);
        });
        /**
         * response completed
         */
        request.onResponseSuccess(theResponse -> {
            //cleanup
            String traceid = theResponse.getRequest().getHeaders().get(TracingConstant.JAEGER_PROPAGATION_KEY);
            filter.getSpanMap().remove(TracingConstant.SPAN_REQUEST_PREFIX + traceid);
            filter.getSpanMap().remove(TracingConstant.SPAN_RESPONSE_PREFIX + traceid);
        });
        return request;
    }

    /**
     * Convert body into a String using the Charset
     * @param buffer body
     * @param charset charset
     * @return the body converted in String
     */
    private static String toString(ByteBuffer buffer, Charset charset) {
        byte[] bytes;
        if (buffer.hasArray()) {
            bytes = new byte[buffer.capacity()];
            System.arraycopy(buffer.array(), 0, bytes, 0, buffer.capacity());
        } else {
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes, 0, bytes.length);
        }
        return new String(bytes, charset);
    }

    /**
     * find the charset
     * @param headers headers
     * @return the charset
     */
    private static Charset getCharset(HttpFields headers) {
        String contentType = headers.get(HttpHeader.CONTENT_TYPE);
        if (contentType != null) {
            String[] tokens = contentType
              .toLowerCase(Locale.US)
              .split("charset=");
            if (tokens.length == 2) {
                String encoding = tokens[1].replaceAll("[;\"]", "");
                return Charset.forName(encoding);
            }
        }
        return StandardCharsets.UTF_8;
    }

}

