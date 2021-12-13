package com.example.opentracing.quarkus.util;

import javax.inject.Inject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.HttpServerRequest;

/**
 * The type Logging filter.
 */
@SuppressWarnings("PackageVisibleField")
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter, ClientRequestFilter, ClientResponseFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingFilter.class);

    @Context
    UriInfo info;

    @Context
    HttpServerRequest request;

    static ObjectMapper mapper;

    /**
     * Instantiates a new Logging filter.
     *
     * @param objectMapper the object mapper
     */
    @Inject
    public LoggingFilter(ObjectMapper objectMapper){
        mapper = objectMapper;
    }

    /**
     * Log the incoming request received (Method + body in json)
     *
     * @param context request context
     */
    @Override
    public void filter(ContainerRequestContext context) {
        final var method = context.getMethod();
        final var path = info.getPath();
        final var address = request.remoteAddress().toString();
        var content = getRequestBody(context);

        logRequestIfNeeded(method, path, address, content);
    }

    /**
     * Log the incoming response sent (Method + body in json)
     *
     * @param containerRequestContext  request context
     * @param containerResponseContext response context
     */
    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
        final var method = containerRequestContext.getMethod();
        final var path = info.getPath();
        final var address = request.remoteAddress().toString();
        final var status = containerResponseContext.getStatus();

        var content = getResponseBody(containerResponseContext);

        logResponseIfNeeded(method, status, address, content);
    }

    /**
     * Log the incoming RestClient request received (Method + body in json)
     *
     * @param clientRequestContext request context
     * @throws IOException if there are errors
     */
    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        final var method = clientRequestContext.getMethod();
        final var address = clientRequestContext.getUri().toString();
        var content = getRequestBody(clientRequestContext);

        logRequestIfNeeded(method, address, content);
    }

    /**
     * Log the incoming RestClient response sent (Method + body in json)
     *
     * @param clientRequestContext  request context
     * @param clientResponseContext response context
     * @throws IOException if there are errors
     */
    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) throws IOException {
        final var method = clientRequestContext.getMethod();
        final var address = clientRequestContext.getUri().toString();
        final var status = clientResponseContext.getStatus();
        var content = getResponseBody(clientResponseContext);

        logResponseIfNeeded(method, status, address, content);
    }

    /**
     * Gets request body.
     *
     * @param clientRequestContext the client request context
     * @return the request body
     */
    public static String getRequestBody(ClientRequestContext clientRequestContext) {
        var content = "";
        try {
            if (clientRequestContext.hasEntity()) {
                var original = mapper.writeValueAsString(clientRequestContext.getEntity());
                content = original.replaceAll("[\r\n]+", " ");
            }
        } catch (Exception ignored) {
        }

        return content;
    }

    /**
     * Gets request body.
     *
     * @param requestContext the request context
     * @return the request body
     */
    public static String getRequestBody(ContainerRequestContext requestContext) {
        var content = "";

        if(requestContext.hasEntity()) {
            try {
                var original = IOUtils.toString(requestContext.getEntityStream(), StandardCharsets.UTF_8);
                content = original.replaceAll("[\r\n]+", " ");
                var in = IOUtils.toInputStream(original, StandardCharsets.UTF_8);
                requestContext.setEntityStream(in);
            } catch (IOException e) {
                content = e.toString();
            }
        }

        return content;
    }

    /**
     * Gets response body.
     *
     * @param clientResponseContext the client response context
     * @return the response body
     */
    public static String getResponseBody(ClientResponseContext clientResponseContext) {
        var content = "";

        if(clientResponseContext.hasEntity()) {
            try {
                var original = IOUtils.toString(clientResponseContext.getEntityStream(), StandardCharsets.UTF_8);
                content = original.replaceAll("[\r\n]+", " ");
                var in = IOUtils.toInputStream(original, StandardCharsets.UTF_8);
                clientResponseContext.setEntityStream(in);
            } catch (IOException ignored) {

            }
        }

        return content;
    }

    /**
     * Gets response body.
     *
     * @param containerResponseContext the container response context
     * @return the response body
     */
    public static String getResponseBody(ContainerResponseContext containerResponseContext) {
        var content = "";
        try {
            if (containerResponseContext.hasEntity()) {
                var original = mapper.writeValueAsString(containerResponseContext.getEntity());
                content = original.replaceAll("[\r\n]+", " ");
            }
        } catch (Exception ignored) {
        }

        return content;
    }

    private void logRequestIfNeeded(String method, String address, String content) {
        // default
        LOGGER.debug("RestClient request [{}] to IP [{}] Body [{}]", method, address, content);
    }

    private void logRequestIfNeeded(String method, String path, String address, String content) {
        // default
        LOGGER.debug("Request [{}] [{}] from IP [{}] Body [{}]", method, path, address, content);
    }

    private void logResponseIfNeeded(String method, int status, String address, String content) {
        // default
        LOGGER.debug("RestClient response [{}] Status code [{}] to IP [{}] Body [{}]", method, status, address, content);
    }


}
