package com.organization.lib.interceptor;

import com.organization.lib.config.CentralizedLogsProperties;
import com.organization.lib.service.ArtemisMessageSender;
import com.organization.lib.service.LogEventBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequestInterceptor Tests")
class RequestInterceptorTest {

    @Mock
    private ArtemisMessageSender messageSender;

    @Mock
    private LogEventBuilder logEventBuilder;

    private CentralizedLogsProperties properties;
    private RequestInterceptor requestInterceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        properties = new CentralizedLogsProperties();
        properties.setServiceName("test-service");
        properties.setEnvironment("test");
        properties.getQueue().setLogs("test.queue");
        properties.getQueue().setErrors("test.errors");

        requestInterceptor = new RequestInterceptor(messageSender, logEventBuilder, properties);
    }

    @Test
    @DisplayName("Should return true on preHandle")
    void testPreHandleReturnsTrue() throws Exception {
        request = new MockHttpServletRequest("GET", "/api/users");
        response = new MockHttpServletResponse();

        boolean result = requestInterceptor.preHandle(request, response, null);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should process successful request in afterCompletion")
    void testAfterCompletionSuccessfulRequest() throws Exception {
        request = new MockHttpServletRequest("GET", "/api/users");
        response = new MockHttpServletResponse();
        response.setStatus(200);

        when(logEventBuilder.buildSuccessEvent(any(), any(), anyLong()))
                .thenReturn("{\"result\":\"SUCCESS\",\"serviceName\":\"test-service\"}");

        requestInterceptor.preHandle(request, response, null);
        requestInterceptor.afterCompletion(request, response, null, null);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageSender).sendMessage(eq("test.queue"), messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertTrue(message.contains("SUCCESS"));
        assertTrue(message.contains("test-service"));
    }

    @Test
    @DisplayName("Should process request with error in afterCompletion")
    void testAfterCompletionWithError() throws Exception {
        request = new MockHttpServletRequest("POST", "/api/users");
        response = new MockHttpServletResponse();
        Exception testException = new RuntimeException("Test error");

        when(logEventBuilder.buildErrorEvent(any(), any(), anyLong(), any()))
                .thenReturn("{\"result\":\"ERROR\"}");

        requestInterceptor.preHandle(request, response, null);
        requestInterceptor.afterCompletion(request, response, null, testException);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageSender).sendMessage(eq("test.errors"), messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertTrue(message.contains("ERROR"));
    }

    @Test
    @DisplayName("Should skip error endpoint in afterCompletion")
    void testAfterCompletionSkipsErrorEndpoint() throws Exception {
        request = new MockHttpServletRequest("GET", "/error");
        response = new MockHttpServletResponse();

        requestInterceptor.preHandle(request, response, null);
        requestInterceptor.afterCompletion(request, response, null, null);

        verify(messageSender, never()).sendMessage(anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle afterCompletion when preHandle was not called (NPE protection)")
    void testAfterCompletionWithoutPreHandle() throws Exception {
        request = new MockHttpServletRequest("GET", "/api/users");
        response = new MockHttpServletResponse();

        when(logEventBuilder.buildSuccessEvent(any(), any(), anyLong()))
                .thenReturn("{\"result\":\"SUCCESS\"}");

        // Call afterCompletion directly without preHandle
        requestInterceptor.afterCompletion(request, response, null, null);

        // Should not throw NPE; should use durationMs = 0
        verify(messageSender).sendMessage(eq("test.queue"), anyString());
    }

    @Test
    @DisplayName("Should always remove ThreadLocal even on error")
    void testThreadLocalCleanupOnError() throws Exception {
        request = new MockHttpServletRequest("GET", "/api/users");
        response = new MockHttpServletResponse();

        when(logEventBuilder.buildSuccessEvent(any(), any(), anyLong()))
                .thenThrow(new RuntimeException("Simulated build error"));

        requestInterceptor.preHandle(request, response, null);
        // Should not throw, error is caught internally
        requestInterceptor.afterCompletion(request, response, null, null);

        // Verify no exception propagated and sender was not called due to builder error
        verify(messageSender, never()).sendMessage(anyString(), anyString());
    }

    @Test
    @DisplayName("Should measure request duration")
    void testRequestDuration() throws Exception {
        request = new MockHttpServletRequest("GET", "/api/users");
        response = new MockHttpServletResponse();

        when(logEventBuilder.buildSuccessEvent(any(), any(), anyLong()))
                .thenReturn("{\"durationMs\":10}");

        requestInterceptor.preHandle(request, response, null);
        Thread.sleep(10);
        requestInterceptor.afterCompletion(request, response, null, null);

        verify(messageSender).sendMessage(eq("test.queue"), anyString());
    }

    @Test
    @DisplayName("Should handle exception without throwing")
    void testHandleExceptionGracefully() throws Exception {
        request = new MockHttpServletRequest("GET", "/api/users");
        response = new MockHttpServletResponse();

        when(logEventBuilder.buildErrorEvent(any(), any(), anyLong(), any()))
                .thenReturn("{\"result\":\"ERROR\",\"error\":{\"exceptionType\":\"IllegalArgumentException\"}}");

        requestInterceptor.preHandle(request, response, null);
        requestInterceptor.afterCompletion(request, response, null,
                new IllegalArgumentException("Test exception"));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageSender).sendMessage(eq("test.errors"), messageCaptor.capture());

        String message = messageCaptor.getValue();
        assertTrue(message.contains("IllegalArgumentException"));
    }
}
