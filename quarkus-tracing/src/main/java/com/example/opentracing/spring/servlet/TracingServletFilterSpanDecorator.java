package com.example.opentracing.spring.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.example.opentracing.TracingConstant;
import io.opentracing.Span;
import io.opentracing.contrib.web.servlet.filter.ServletFilterSpanDecorator;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class TracingServletFilterSpanDecorator implements ServletFilterSpanDecorator {

    private Map<String,Span> spanMap = new HashMap<>();

    @Override
    public void onRequest(HttpServletRequest httpServletRequest, Span span) {
        String uuid = UUID.randomUUID().toString();

        spanMap.put(TracingConstant.SPAN_REQUEST_PREFIX + uuid, span);

        httpServletRequest.setAttribute(TracingConstant.JAEGER_PROPAGATION_KEY, uuid);
    }

    @Override
    public void onResponse(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Span span) {
        spanMap.put(TracingConstant.SPAN_RESPONSE_PREFIX + httpServletResponse.hashCode(), span);
    }

    @Override
    public void onError(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Throwable throwable, Span span) {
        span.setTag("ServletFilterSpanDecorator onError", throwable.toString());
    }

    @Override
    public void onTimeout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, long l, Span span) {
        span.setTag("ServletFilterSpanDecorator onTimeout", l);
    }

    public Map<String, Span> getSpanMap() {
        return spanMap;
    }
}
