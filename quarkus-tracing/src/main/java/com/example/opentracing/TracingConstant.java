package com.example.opentracing;

public class TracingConstant {

    public static final String JAEGER_PROPAGATION_KEY = "uber-trace-id";
    public static final String TRACING_HTTP_HEADERS = "http.headers";
    public static final String TRACING_HTTP_REQUEST_BODY = "http.request.body";
    public static final String TRACING_HTTP_RESPONSE_BODY = "http.response.body";

    public static final String SPAN_REQUEST_PREFIX = "request-";
    public static final String SPAN_RESPONSE_PREFIX = "response-";

}
