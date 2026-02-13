package com.organization.lib.service;

import com.organization.lib.config.CentralizedLogsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link ArtemisMessageSender} that sends messages to
 * Apache ActiveMQ Artemis queues via JMS.
 * <p>
 * Message sending can be disabled via the
 * {@code centralized-logs.sender.enabled} property.
 * When disabled, messages are logged at debug level but not sent to the broker.
 * <p>
 * If Artemis is unavailable, errors are logged but <strong>not
 * propagated</strong>
 * to avoid disrupting the application's normal HTTP response flow.
 *
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "centralized-logs.sender.enabled", havingValue = "true", matchIfMissing = true)
public class ArtemisMessageSenderImpl implements ArtemisMessageSender {

    private final JmsTemplate jmsTemplate;
    private final CentralizedLogsProperties properties;

    /**
     * {@inheritDoc}
     * <p>
     * If the sender is disabled, the message is logged at debug level and not sent.
     * If Artemis is unavailable, the error is logged but not propagated.
     */
    @Override
    public void sendMessage(String queueName, String message) {
        if (!properties.getSender().isEnabled()) {
            log.debug("Sender disabled, message not sent to queue '{}': {}", queueName, message);
            return;
        }
        try {
            jmsTemplate.convertAndSend(queueName, message);
            log.debug("Message sent to queue '{}': {}", queueName, message);
        } catch (Exception e) {
            log.error("Artemis unavailable, message not sent to queue '{}': {}", queueName, e.getMessage());
        }
    }
}