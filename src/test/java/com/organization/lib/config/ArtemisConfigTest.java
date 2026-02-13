package com.organization.lib.config;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArtemisConfig Tests")
class ArtemisConfigTest {

    private CentralizedLogsProperties properties;
    private ArtemisConfig artemisConfig;

    @BeforeEach
    void setUp() {
        properties = new CentralizedLogsProperties();
        properties.setBrokerUrl("tcp://localhost:61616");
        properties.setUser("artemis");
        properties.setPassword("artemis");
        artemisConfig = new ArtemisConfig(properties);
    }

    @Test
    @DisplayName("Should create ConnectionFactory bean")
    void testConnectionFactoryBean() {
        ConnectionFactory connectionFactory = artemisConfig.connectionFactory();
        assertNotNull(connectionFactory);
    }

    @Test
    @DisplayName("Should create ActiveMQConnectionFactory")
    void testConnectionFactoryIsActiveMQ() {
        ConnectionFactory connectionFactory = artemisConfig.connectionFactory();
        assertInstanceOf(ActiveMQConnectionFactory.class, connectionFactory);
    }

    @Test
    @DisplayName("Should create ConnectionFactory with custom URL")
    void testConnectionFactoryWithCustomUrl() {
        properties.setBrokerUrl("tcp://artemis-server:61616");

        ConnectionFactory connectionFactory = artemisConfig.connectionFactory();
        assertNotNull(connectionFactory);
        assertInstanceOf(ActiveMQConnectionFactory.class, connectionFactory);
    }

    @Test
    @DisplayName("Should create ConnectionFactory with custom credentials")
    void testConnectionFactoryWithCustomCredentials() {
        properties.setUser("customUser");
        properties.setPassword("customPassword");

        ConnectionFactory connectionFactory = artemisConfig.connectionFactory();
        assertNotNull(connectionFactory);
        assertInstanceOf(ActiveMQConnectionFactory.class, connectionFactory);
    }

    @Test
    @DisplayName("Should create multiple ConnectionFactory instances")
    void testMultipleConnectionFactoryInstances() {
        ConnectionFactory cf1 = artemisConfig.connectionFactory();
        ConnectionFactory cf2 = artemisConfig.connectionFactory();

        assertNotNull(cf1);
        assertNotNull(cf2);
    }
}
