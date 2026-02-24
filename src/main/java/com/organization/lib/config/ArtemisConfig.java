package com.organization.lib.config;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;

/**
 * Configuration for the JMS connection to Apache ActiveMQ Artemis.
 * <p>
 * Creates an {@link ActiveMQConnectionFactory} bean using the connection
 * properties
 * defined in {@link CentralizedLogsProperties}. The bean is only created if:
 * <ul>
 * <li>No other {@link ConnectionFactory} bean exists
 * ({@code @ConditionalOnMissingBean})</li>
 * <li>The sender is enabled ({@code centralized-logs.sender.enabled=true})</li>
 * </ul>
 *
 * @since 1.0.0
 * @see CentralizedLogsProperties
 */
@Configuration
@EnableJms
@RequiredArgsConstructor
@ConditionalOnProperty(name = "centralized-logs.sender.enabled", havingValue = "true", matchIfMissing = true)
public class ArtemisConfig {

    private final CentralizedLogsProperties properties;

    /**
     * Creates the JMS {@link ConnectionFactory} for Artemis.
     * <p>
     * This bean is only registered if no other {@link ConnectionFactory} is already
     * present,
     * allowing consumers to provide their own connection factory if needed.
     *
     * @return configured {@link ActiveMQConnectionFactory}
     */
    @Bean
    @ConditionalOnMissingBean(ConnectionFactory.class)
    public ConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(properties.getBrokerUrl());
        connectionFactory.setUser(properties.getUser());
        connectionFactory.setPassword(properties.getPassword());
        return connectionFactory;
    }
}