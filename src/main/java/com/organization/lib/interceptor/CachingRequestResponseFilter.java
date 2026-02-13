package com.organization.lib.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Servlet filter that wraps the HTTP request and response with caching
 * wrappers,
 * allowing the request/response body to be read multiple times.
 * <p>
 * This is necessary because the default {@link HttpServletRequest} and
 * {@link HttpServletResponse} only allow the body to be read once. The
 * {@link ContentCachingRequestWrapper} and
 * {@link ContentCachingResponseWrapper}
 * cache the body content for subsequent reads by the
 * {@link RequestInterceptor}.
 * <p>
 * Runs with {@link Ordered#HIGHEST_PRECEDENCE} to ensure wrapping occurs before
 * any other filter processes the request.
 * <p>
 * Conditionally enabled via {@code centralized-logs.interceptor.enabled}
 * (default: {@code true}).
 *
 * @since 1.0.0
 * @see RequestInterceptor
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "centralized-logs.interceptor.enabled", havingValue = "true", matchIfMissing = true)
public class CachingRequestResponseFilter extends OncePerRequestFilter {

    /**
     * Wraps the request and response with caching wrappers, then proceeds with
     * the filter chain. After the chain completes, copies the cached response
     * body back to the original response.
     *
     * @param request  the incoming HTTP request
     * @param response the outgoing HTTP response
     * @param chain    the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        chain.doFilter(wrappedRequest, wrappedResponse);

        wrappedResponse.copyBodyToResponse();
    }
}