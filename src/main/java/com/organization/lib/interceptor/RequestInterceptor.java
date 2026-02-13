package com.organization.lib.interceptor;

import com.organization.lib.config.CentralizedLogsProperties;
import com.organization.lib.service.ArtemisMessageSender;
import com.organization.lib.service.LogEventBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor that captures HTTP request/response data and publishes
 * structured log events to Apache ActiveMQ Artemis queues.
 * <p>
 * For each incoming request, this interceptor:
 * <ul>
 * <li>Records the start timestamp in {@code preHandle}</li>
 * <li>After the controller processes the request, builds a JSON log event</li>
 * <li>Sends success events to the configured logs queue, and error events to
 * the errors queue</li>
 * </ul>
 * <p>
 * Requests to the {@code /error} endpoint are automatically skipped.
 * Additional paths can be excluded via
 * {@code centralized-logs.interceptor.exclude-paths}.
 * <p>
 * This interceptor is conditionally registered based on the
 * {@code centralized-logs.interceptor.enabled} property (default:
 * {@code true}).
 *
 * @since 1.0.0
 * @see LogEventBuilder
 * @see ArtemisMessageSender
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "centralized-logs.interceptor.enabled", havingValue = "true", matchIfMissing = true)
public class RequestInterceptor implements HandlerInterceptor {

    private final ArtemisMessageSender messageSender;
    private final LogEventBuilder logEventBuilder;
    private final CentralizedLogsProperties properties;

    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    /**
     * Records the start time of the request and logs the incoming method and URI.
     *
     * @return always {@code true}, allowing the request to proceed
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {
        startTime.set(System.currentTimeMillis());
        log.info("Intercepting request: {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    /**
     * Called after the request has been fully processed. Builds a structured JSON
     * log event
     * and sends it to the appropriate Artemis queue. If an exception occurred, the
     * event
     * is sent to the errors queue; otherwise, to the logs queue.
     * <p>
     * This method is designed to never throw exceptions to avoid interfering with
     * the
     * normal HTTP response flow. All internal errors are caught and logged.
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) throws Exception {
        try {
            if (request.getRequestURI().contains("/error")) {
                return;
            }

            Long start = startTime.get();
            long durationMs = (start != null) ? System.currentTimeMillis() - start : 0;

            if (ex != null) {
                String errorMessage = logEventBuilder.buildErrorEvent(request, response, durationMs, ex);
                log.error("Error in request: {} {}", request.getMethod(), request.getRequestURI());
                messageSender.sendMessage(properties.getQueue().getErrors(), errorMessage);
            } else {
                String logMessage = logEventBuilder.buildSuccessEvent(request, response, durationMs);
                messageSender.sendMessage(properties.getQueue().getLogs(), logMessage);
            }

        } catch (Exception e) {
            log.error("Error building log event: {}", e.getMessage());
        } finally {
            startTime.remove();
        }
    }
}