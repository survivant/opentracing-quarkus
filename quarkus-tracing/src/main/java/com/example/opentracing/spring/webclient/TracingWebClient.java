package com.example.opentracing.spring.webclient;

import java.net.URI;


import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class TracingWebClient {

    @Autowired
    private LoggingTracingWebClientFilterFunction loggingTracingWebClientFilterFunction;

    public WebClient getWebClient(String url){
        // need this part to be able to read request/response body
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        HttpClient httpClient = new HttpClient(sslContextFactory) {
            @Override
            public Request newRequest(URI uri) {
                Request request = super.newRequest(uri);
                return new WebClientBodyExtractor().init(request, loggingTracingWebClientFilterFunction);
            }
        };

        WebClient webClient = WebClient
                .builder()
                .clientConnector(new JettyClientHttpConnector(httpClient))
                .filter(loggingTracingWebClientFilterFunction)
                .baseUrl(url)
                .build();

        return webClient;
    }
}
