package com.organization.lib.config;

import com.organization.lib.interceptor.RequestInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterceptorConfig Tests")
class InterceptorConfigTest {

    @Mock
    private RequestInterceptor requestInterceptor;

    @Mock
    private InterceptorRegistry registry;

    @Mock
    private InterceptorRegistration interceptorRegistration;

    private CentralizedLogsProperties properties;
    private InterceptorConfig interceptorConfig;

    @BeforeEach
    void setUp() {
        properties = new CentralizedLogsProperties();
        interceptorConfig = new InterceptorConfig(requestInterceptor, properties);
    }

    private void mockRegistryChain() {
        when(registry.addInterceptor(any())).thenReturn(interceptorRegistration);
        when(interceptorRegistration.addPathPatterns(any(String.class))).thenReturn(interceptorRegistration);
        when(interceptorRegistration.excludePathPatterns(any(List.class))).thenReturn(interceptorRegistration);
        when(interceptorRegistration.order(anyInt())).thenReturn(interceptorRegistration);
    }

    @Test
    @DisplayName("Should add interceptor with path patterns")
    void testAddInterceptor() {
        mockRegistryChain();

        interceptorConfig.addInterceptors(registry);

        verify(registry).addInterceptor(requestInterceptor);
        verify(interceptorRegistration).addPathPatterns("/**");
        verify(interceptorRegistration).order(1);
    }

    @Test
    @DisplayName("Should configure interceptor with correct path patterns")
    void testInterceptorPathPatterns() {
        mockRegistryChain();

        interceptorConfig.addInterceptors(registry);

        verify(interceptorRegistration).addPathPatterns("/**");
    }

    @Test
    @DisplayName("Should exclude configured paths")
    void testExcludeConfiguredPaths() {
        mockRegistryChain();
        properties.getInterceptor().setExcludePaths(List.of("/actuator/**", "/health"));

        interceptorConfig.addInterceptors(registry);

        verify(interceptorRegistration).excludePathPatterns(List.of("/actuator/**", "/health"));
    }

    @Test
    @DisplayName("Should handle custom exclude paths")
    void testCustomExcludePaths() {
        mockRegistryChain();
        properties.getInterceptor().setExcludePaths(List.of("/custom/**", "/internal/**"));

        interceptorConfig.addInterceptors(registry);

        verify(interceptorRegistration).excludePathPatterns(List.of("/custom/**", "/internal/**"));
    }
}