package com.organization.lib.config;

import com.organization.lib.interceptor.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link RequestInterceptor} in the Spring MVC interceptor chain.
 * <p>
 * Registration is controlled by the
 * {@code centralized-logs.interceptor.enabled} property.
 * When disabled, no interceptor is added and HTTP requests flow directly to the
 * controllers.
 * <p>
 * Paths to exclude from interception can be configured via
 * {@code centralized-logs.interceptor.exclude-paths}.
 *
 * @since 1.0.0
 * @see RequestInterceptor
 * @see CentralizedLogsProperties
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "centralized-logs.interceptor.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(RequestInterceptor.class)
public class InterceptorConfig implements WebMvcConfigurer {

    private final RequestInterceptor requestInterceptor;
    private final CentralizedLogsProperties properties;

    /**
     * Registers the {@link RequestInterceptor} to intercept all paths
     * ({@code /**}),
     * excluding configured paths such as actuator, health, and swagger endpoints.
     *
     * @param registry the interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(properties.getInterceptor().getExcludePaths())
                .order(1);
        log.info("Centralized Logs interceptor ENABLED (excluding: {})", properties.getInterceptor().getExcludePaths());
    }
}