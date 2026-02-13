package com.organization.lib.service;

import com.organization.lib.config.CentralizedLogsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArtemisMessageSenderImpl Tests")
class ArtemisMessageSenderImplTest {

    @Mock
    private JmsTemplate jmsTemplate;

    private CentralizedLogsProperties properties;
    private ArtemisMessageSenderImpl messageSender;

    private static final String TEST_QUEUE = "test.queue";
    private static final String TEST_MESSAGE = "Test message";

    @BeforeEach
    void setUp() {
        properties = new CentralizedLogsProperties();
        properties.getSender().setEnabled(true);
        messageSender = new ArtemisMessageSenderImpl(jmsTemplate, properties);
    }

    @Test
    @DisplayName("Should send message successfully")
    void testSendMessageSuccess() {
        messageSender.sendMessage(TEST_QUEUE, TEST_MESSAGE);
        verify(jmsTemplate).convertAndSend(TEST_QUEUE, TEST_MESSAGE);
    }

    @Test
    @DisplayName("Should not send message when sender is disabled")
    void testSendMessageWhenDisabled() {
        properties.getSender().setEnabled(false);
        messageSender.sendMessage(TEST_QUEUE, TEST_MESSAGE);
        verify(jmsTemplate, never()).convertAndSend(anyString(), anyString());
    }

    @Test
    @DisplayName("Should not throw RuntimeException when send fails - just log the error")
    void testSendMessageDoesNotThrowException() {
        doThrow(new RuntimeException("JMS error")).when(jmsTemplate)
                .convertAndSend(TEST_QUEUE, TEST_MESSAGE);

        // Should NOT throw - logging should never break the application
        assertDoesNotThrow(() -> messageSender.sendMessage(TEST_QUEUE, TEST_MESSAGE));
    }

    @Test
    @DisplayName("Should send empty string message")
    void testSendEmptyStringMessage() {
        messageSender.sendMessage(TEST_QUEUE, "");
        verify(jmsTemplate).convertAndSend(TEST_QUEUE, "");
    }
}
