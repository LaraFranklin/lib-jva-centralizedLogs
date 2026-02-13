package com.organization.lib.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.organization.lib.config.CentralizedLogsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LogEventBuilder Tests")
class LogEventBuilderTest {

    private ObjectMapper objectMapper;
    private CentralizedLogsProperties properties;
    private LogEventBuilder logEventBuilder;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        properties = new CentralizedLogsProperties();
        properties.setServiceName("test-service");
        properties.setEnvironment("test");
        properties.setBodyMaxSize(100);
        logEventBuilder = new LogEventBuilder(objectMapper, properties);
    }

    @Test
    @DisplayName("Should build success event with correct fields")
    void testBuildSuccessEvent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        String json = logEventBuilder.buildSuccessEvent(request, response, 42);

        JsonNode node = objectMapper.readTree(json);
        assertEquals("SUCCESS", node.get("result").asText());
        assertEquals("test-service", node.get("serviceName").asText());
        assertEquals("test", node.get("environment").asText());
        assertEquals(42, node.get("durationMs").asInt());
        assertNotNull(node.get("eventId").asText());
        assertNotNull(node.get("timestamp").asText());
        assertNotNull(node.get("correlationId").asText());
    }

    @Test
    @DisplayName("Should build error event with exception details")
    void testBuildErrorEvent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Exception ex = new IllegalArgumentException("Bad input");

        String json = logEventBuilder.buildErrorEvent(request, response, 10, ex);

        JsonNode node = objectMapper.readTree(json);
        assertEquals("ERROR", node.get("result").asText());
        assertNotNull(node.get("error"));
        assertEquals("IllegalArgumentException", node.get("error").get("exceptionType").asText());
        assertEquals("Bad input", node.get("error").get("message").asText());
    }

    @Test
    @DisplayName("Should capture HTTP method and path")
    void testCaptureHttpInfo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/users/123");
        request.setQueryString("fields=name");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        String json = logEventBuilder.buildSuccessEvent(request, response, 5);

        JsonNode node = objectMapper.readTree(json);
        assertEquals("PUT", node.path("http").get("method").asText());
        assertEquals("/api/users/123", node.path("http").get("path").asText());
        assertEquals("fields=name", node.path("http").get("queryString").asText());
    }

    @Test
    @DisplayName("Should capture client info")
    void testCaptureClientInfo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("user-agent", "Mozilla/5.0");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String json = logEventBuilder.buildSuccessEvent(request, response, 5);

        JsonNode node = objectMapper.readTree(json);
        assertEquals("192.168.1.100", node.path("client").get("ip").asText());
        assertEquals("Mozilla/5.0", node.path("client").get("userAgent").asText());
    }

    @Test
    @DisplayName("Should use correlation id from header")
    void testCorrelationIdFromHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("x-correlation-id", "my-corr-id-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String json = logEventBuilder.buildSuccessEvent(request, response, 5);

        JsonNode node = objectMapper.readTree(json);
        assertEquals("my-corr-id-123", node.get("correlationId").asText());
    }

    @Test
    @DisplayName("Should generate correlation id when header is missing")
    void testCorrelationIdGenerated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String json = logEventBuilder.buildSuccessEvent(request, response, 5);

        JsonNode node = objectMapper.readTree(json);
        assertNotNull(node.get("correlationId").asText());
        assertTrue(node.get("correlationId").asText().length() > 0);
    }

    @Test
    @DisplayName("Should capture request headers")
    void testCaptureRequestHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        request.addHeader("content-type", "application/json");
        request.addHeader("authorization", "Bearer token123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String json = logEventBuilder.buildSuccessEvent(request, response, 5);

        JsonNode node = objectMapper.readTree(json);
        assertEquals("application/json", node.path("request").path("headers").get("content-type").asText());
    }

    @Test
    @DisplayName("Should handle error with cause")
    void testErrorWithCause() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Exception cause = new IllegalStateException("Root cause");
        Exception ex = new RuntimeException("Wrapper", cause);

        String json = logEventBuilder.buildErrorEvent(request, response, 10, ex);

        JsonNode node = objectMapper.readTree(json);
        assertEquals("Root cause", node.path("error").get("cause").asText());
    }

    @Test
    @DisplayName("Should handle empty request and response body")
    void testEmptyBodies() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String json = logEventBuilder.buildSuccessEvent(request, response, 5);

        JsonNode node = objectMapper.readTree(json);
        assertNotNull(node.path("request").get("body"));
        assertNotNull(node.path("response").get("body"));
    }
}
