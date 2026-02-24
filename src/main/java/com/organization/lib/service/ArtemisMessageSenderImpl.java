package com.organization.lib.service;

import com.organization.lib.config.CentralizedLogsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

/**
 * Implementation of {@link ArtemisMessageSender} that sends messages to
 * Apache ActiveMQ Artemis queues via JMS.
 * <p>
 * Supports optional GZIP compression via
 * {@code centralized-logs.compression.enabled}.
 * When compression is enabled, messages are sent as JMS {@code BytesMessage}
 * with
 * GZIP-compressed content. When disabled (default), messages are sent as plain
 * text.
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
     * If compression is enabled, the message is GZIP-compressed and sent as a
     * {@code BytesMessage}. Otherwise, it is sent as a plain {@code TextMessage}.
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
            if (properties.getCompression().isEnabled()) {
                byte[] compressed = compress(message);
                jmsTemplate.send(queueName, session -> {
                    var bytesMessage = session.createBytesMessage();
                    bytesMessage.writeBytes(compressed);
                    bytesMessage.setStringProperty("Content-Encoding", "gzip");
                    return bytesMessage;
                });
                log.debug("Compressed message sent to queue '{}' ({} bytes -> {} bytes)",
                        queueName, message.getBytes(StandardCharsets.UTF_8).length, compressed.length);
            } else {
                jmsTemplate.convertAndSend(queueName, message);
                log.debug("Message sent to queue '{}': {}", queueName, message);
            }
        } catch (Exception e) {
            log.error("Artemis unavailable, message not sent to queue '{}': {}", queueName, e.getMessage());
        }
    }

    /**
     * Compresses a string using GZIP.
     *
     * @param data the string to compress
     * @return the GZIP-compressed bytes
     * @throws Exception if compression fails
     */
    private byte[] compress(String data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }
}