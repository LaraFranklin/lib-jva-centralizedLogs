package com.organization.lib.service;

/**
 * Interface for sending messages to Apache ActiveMQ Artemis queues.
 * <p>
 * Implementations should handle connection management, error handling,
 * and conditional sending based on configuration.
 *
 * @since 1.0.0
 * @see ArtemisMessageSenderImpl
 */
public interface ArtemisMessageSender {

    /**
     * Sends a text message to the specified Artemis queue.
     *
     * @param queueName the name of the target queue
     * @param message   the text message to send
     */
    void sendMessage(String queueName, String message);
}
