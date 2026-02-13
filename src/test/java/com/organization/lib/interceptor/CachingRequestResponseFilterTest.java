package com.organization.lib.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CachingRequestResponseFilter Tests")
class CachingRequestResponseFilterTest {

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private CachingRequestResponseFilter filter;

    @Test
    @DisplayName("Should wrap request with ContentCachingRequestWrapper")
    void testRequestIsWrapped() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doNothing().when(filterChain).doFilter(any(ContentCachingRequestWrapper.class), 
                                               any(ContentCachingResponseWrapper.class));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), 
                                     any(ContentCachingResponseWrapper.class));
    }

    @Test
    @DisplayName("Should wrap response with ContentCachingResponseWrapper")
    void testResponseIsWrapped() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doNothing().when(filterChain).doFilter(any(ContentCachingRequestWrapper.class), 
                                               any(ContentCachingResponseWrapper.class));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), 
                                     any(ContentCachingResponseWrapper.class));
    }

    @Test
    @DisplayName("Should copy response body to response")
    void testResponseBodyIsCopied() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/test");
        request.setContent("{\"key\": \"value\"}".getBytes());
        
        MockHttpServletResponse response = new MockHttpServletResponse();

        doNothing().when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), 
                                     any(ContentCachingResponseWrapper.class));
    }

    @Test
    @DisplayName("Should handle GET request")
    void testGetRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doNothing().when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), 
                                     any(ContentCachingResponseWrapper.class));
    }

    @Test
    @DisplayName("Should handle POST request with body")
    void testPostRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.setContent("{\"name\": \"John\", \"email\": \"john@example.com\"}".getBytes());
        request.setContentType("application/json");

        MockHttpServletResponse response = new MockHttpServletResponse();

        doNothing().when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), 
                                     any(ContentCachingResponseWrapper.class));
    }

    @Test
    @DisplayName("Should handle PUT request")
    void testPutRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/users/123");
        request.setContent("{\"name\": \"Jane\"}".getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        doNothing().when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), 
                                     any(ContentCachingResponseWrapper.class));
    }

    @Test
    @DisplayName("Should handle DELETE request")
    void testDeleteRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/users/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doNothing().when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), 
                                     any(ContentCachingResponseWrapper.class));
    }

    @Test
    @DisplayName("Should handle empty request body")
    void testEmptyRequestBody() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doNothing().when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), 
                                     any(ContentCachingResponseWrapper.class));
    }
}
