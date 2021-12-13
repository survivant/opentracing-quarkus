package com.example.opentracing.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;

@Configuration
public class TracerConfig {

    @Bean
    public JaegerTracer jaegerTracer() {

        return new io.jaegertracing.Configuration("jaeger-client")
                .withSampler(new io.jaegertracing.Configuration.SamplerConfiguration().withType(ConstSampler.TYPE)
                        .withParam(1))
                .withReporter(new io.jaegertracing.Configuration.ReporterConfiguration().withLogSpans(false)) // if you want to have logs in jaeger
                .getTracer();
    }
}
