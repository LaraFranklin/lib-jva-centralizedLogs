package com.organization.lib.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for the centralized logging library.
 * <p>
 * This class is automatically discovered by Spring Boot via the
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * file.
 * Consumers of this library no longer need to add {@code @ComponentScan}
 * manually.
 *
 * @since 2.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(CentralizedLogsProperties.class)
@ComponentScan(basePackages = "com.organization.lib")
public class CentralizedLogsAutoConfiguration {
}
