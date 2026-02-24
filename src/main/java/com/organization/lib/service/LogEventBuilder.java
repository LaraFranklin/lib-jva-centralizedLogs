package com.organization.lib.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.organization.lib.config.CentralizedLogsProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Builds structured JSON log events from HTTP request/response pairs.
 * <p>
 * Responsible for constructing the JSON payload that is published to Artemis
 * queues.
 * Supports both success and error events, includes correlation ID resolution,
 * body truncation based on configurable max size, and header extraction.
 *
 * @since 2.0.0
 */
@Component
@RequiredArgsConstructor
public class LogEventBuilder {

    private final ObjectMapper objectMapper;
    private final CentralizedLogsProperties properties;

    /**
     * Builds a complete log event JSON for a successful request.
     *
     * @param request    the HTTP request
     * @param response   the HTTP response
     * @param durationMs time taken to process the request in milliseconds
     * @return JSON string representing the log event with result "SUCCESS"
     * @throws Exception if JSON serialization fails
     */
    public String buildSuccessEvent(HttpServletRequest request,
            HttpServletResponse response,
            long durationMs) throws Exception {
        ObjectNode logEvent = buildBaseEvent(request, response, durationMs);
        logEvent.put("result", "SUCCESS");
        return objectMapper.writeValueAsString(logEvent);
    }

    /**
     * Builds a complete log event JSON for a failed request, including error
     * details.
     *
     * @param request    the HTTP request
     * @param response   the HTTP response
     * @param durationMs time taken to process the request in milliseconds
     * @param ex         the exception that caused the error
     * @return JSON string representing the log event with result "ERROR" and error
     *         details
     * @throws Exception if JSON serialization fails
     */
    public String buildErrorEvent(HttpServletRequest request,
            HttpServletResponse response,
            long durationMs,
            Exception ex) throws Exception {
        ObjectNode logEvent = buildBaseEvent(request, response, durationMs);

        int realStatus = resolveErrorStatus(request, ex);
        ((ObjectNode) logEvent.path("http")).put("statusCode", realStatus);

        logEvent.set("error", buildErrorNode(ex));
        logEvent.put("result", "ERROR");
        return objectMapper.writeValueAsString(logEvent);
    }

    private ObjectNode buildBaseEvent(HttpServletRequest request,
            HttpServletResponse response,
            long durationMs) throws Exception {

        String rawRequest = extractRequestBody(request);
        String rawResponse = extractResponseBody(response);
        String correlationId = resolveCorrelationId(request);

        ObjectNode requestHeaders = objectMapper.createObjectNode();
        Collections.list(request.getHeaderNames())
                .forEach(name -> requestHeaders.put(name, request.getHeader(name)));

        ObjectNode responseHeaders = objectMapper.createObjectNode();
        response.getHeaderNames()
                .forEach(name -> responseHeaders.put(name, response.getHeader(name)));

        Object requestBodyNode = parseJson(rawRequest);
        Object responseBodyNode = parseJson(rawResponse);

        ObjectNode logEvent = objectMapper.createObjectNode();
        logEvent.put("eventId", UUID.randomUUID().toString());
        logEvent.put("timestamp", Instant.now().toString());
        logEvent.put("serviceName", properties.getServiceName());
        logEvent.put("environment", properties.getEnvironment());
        logEvent.put("correlationId", correlationId);
        logEvent.put("serviceId",
                properties.getEndpointMappings().get(request.getMethod() + " " + request.getRequestURI()));

        ObjectNode http = objectMapper.createObjectNode();
        http.put("method", request.getMethod());
        http.put("path", request.getRequestURI());
        http.put("queryString", request.getQueryString() != null ? request.getQueryString() : "");
        http.put("statusCode", response.getStatus());
        logEvent.set("http", http);

        logEvent.put("durationMs", durationMs);

        ObjectNode client = objectMapper.createObjectNode();
        client.put("ip", request.getRemoteAddr());
        client.put("userAgent", request.getHeader("user-agent") != null ? request.getHeader("user-agent") : "");
        logEvent.set("client", client);

        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.set("headers", requestHeaders);
        requestNode.set("body", objectMapper.valueToTree(requestBodyNode));
        logEvent.set("request", requestNode);

        ObjectNode responseNode = objectMapper.createObjectNode();
        responseNode.set("headers", responseHeaders);
        responseNode.set("body", objectMapper.valueToTree(responseBodyNode));
        logEvent.set("response", responseNode);

        return logEvent;
    }

    private ObjectNode buildErrorNode(Exception ex) {
        ObjectNode errorNode = objectMapper.createObjectNode();
        errorNode.put("exceptionType", ex.getClass().getSimpleName());
        errorNode.put("message", ex.getMessage() != null ? ex.getMessage() : "Unknown error");

        StringBuilder stackTrace = new StringBuilder();
        StackTraceElement[] elements = ex.getStackTrace();
        int limit = Math.min(elements.length, 5);
        for (int i = 0; i < limit; i++) {
            stackTrace.append(elements[i].toString()).append("\n");
        }
        errorNode.put("stackTrace", stackTrace.toString());

        if (ex.getCause() != null) {
            errorNode.put("cause", ex.getCause().getMessage());
        }

        return errorNode;
    }

    private String extractRequestBody(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                return new String(buf, StandardCharsets.UTF_8);
            }
        }
        return "[empty]";
    }

    private String extractResponseBody(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            byte[] buf = wrapper.getContentAsByteArray();
            if (buf.length > 0) {
                return new String(buf, StandardCharsets.UTF_8);
            }
        }
        return "[empty]";
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader("x-correlation-id");
        return correlationId != null ? correlationId : UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Resolves the endpoint identifier for a given request.
     * <p>
     * Looks up the configured {@code endpoint-mappings} for a match using the
     * format
     * {@code "METHOD /path"}. Supports wildcard {@code *} for path segments.
     * If no mapping matches, generates a default ID in the format
     * {@code METHOD_/path}.
     *
     * @param request the HTTP request
     * @return the resolved endpoint identifier
     */
    private String resolveEndpointId(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String key = method + " " + path;

        Map<String, String> mappings = properties.getEndpointMappings();

        // Exact match
        if (mappings.containsKey(key)) {
            return mappings.get(key);
        }

        // Wildcard match: "GET /api/users/*" matches "GET /api/users/123"
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            if (matchesWildcard(entry.getKey(), key)) {
                return entry.getValue();
            }
        }

        // Auto-generate: "GET /api/users" → "GET_/api/users"
        return method + "_" + path;
    }

    /**
     * Matches a wildcard pattern against a value.
     * Supports {@code *} as a single path segment wildcard and {@code **} as a
     * multi-segment wildcard.
     *
     * @param pattern the pattern (e.g., "GET /api/users/*")
     * @param value   the value to match (e.g., "GET /api/users/123")
     * @return true if the pattern matches the value
     */
    private boolean matchesWildcard(String pattern, String value) {
        String[] patternParts = pattern.split("/");
        String[] valueParts = value.split("/");

        if (patternParts.length != valueParts.length) {
            // Check for ** (match any number of remaining segments)
            if (pattern.contains("/**")) {
                String prefix = pattern.substring(0, pattern.indexOf("/**"));
                return value.startsWith(prefix);
            }
            return false;
        }

        for (int i = 0; i < patternParts.length; i++) {
            if (!patternParts[i].equals("*") && !patternParts[i].equals(valueParts[i])) {
                return false;
            }
        }
        return true;
    }

    private Object parseJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return raw;
        }
    }

    private int resolveErrorStatus(HttpServletRequest request, Exception ex) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        return statusCode != null ? statusCode : 500;
    }
}
