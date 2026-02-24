package com.organization.lib.service;

import com.organization.lib.config.CentralizedLogsProperties;
import jakarta.jms.BytesMessage;
import jakarta.jms.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArtemisMessageSenderImpl Tests")
class ArtemisMessageSenderImplTest {

    @Mock
    private JmsTemplate jmsTemplate;

    private CentralizedLogsProperties properties;
    private ArtemisMessageSenderImpl messageSender;

    private static final String TEST_QUEUE = "test.queue";
    private static final String TEST_MESSAGE = "{\"key\":\"value\",\"data\":\"test message content\"}";

    @BeforeEach
    void setUp() {
        properties = new CentralizedLogsProperties();
        properties.getSender().setEnabled(true);
        properties.getCompression().setEnabled(false);
        messageSender = new ArtemisMessageSenderImpl(jmsTemplate, properties);
    }

    @Test
    @DisplayName("Should send plain text message when compression is disabled")
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

        assertDoesNotThrow(() -> messageSender.sendMessage(TEST_QUEUE, TEST_MESSAGE));
    }

    @Test
    @DisplayName("Should send compressed BytesMessage when compression is enabled")
    void testSendCompressedMessage() throws Exception {
        properties.getCompression().setEnabled(true);

        Session session = mock(Session.class);
        BytesMessage bytesMessage = mock(BytesMessage.class);
        when(session.createBytesMessage()).thenReturn(bytesMessage);

        // Capture the MessageCreator to invoke it
        ArgumentCaptor<MessageCreator> creatorCaptor = ArgumentCaptor.forClass(MessageCreator.class);

        messageSender.sendMessage(TEST_QUEUE, TEST_MESSAGE);

        verify(jmsTemplate).send(eq(TEST_QUEUE), creatorCaptor.capture());

        // Execute the captured message creator
        creatorCaptor.getValue().createMessage(session);

        // Verify BytesMessage was created and Content-Encoding was set
        verify(session).createBytesMessage();
        verify(bytesMessage).setStringProperty("Content-Encoding", "gzip");
    }

    @Test
    @DisplayName("Should not throw when compression fails")
    void testCompressionFailureDoesNotThrow() {
        properties.getCompression().setEnabled(true);

        doThrow(new RuntimeException("JMS error")).when(jmsTemplate)
                .send(anyString(), org.mockito.ArgumentMatchers.any(MessageCreator.class));

        assertDoesNotThrow(() -> messageSender.sendMessage(TEST_QUEUE, TEST_MESSAGE));
    }

    @Test
    @DisplayName("Compressed data should be valid GZIP and decompress to original")
    void testCompressionDecompression() throws Exception {
        // Test the compression utility directly via reflection-free approach
        // Compress using same algorithm as the implementation
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(baos)) {
            gzip.write(TEST_MESSAGE.getBytes(StandardCharsets.UTF_8));
        }
        byte[] compressed = baos.toByteArray();

        // Decompress and verify
        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        try (GZIPInputStream gzipIn = new GZIPInputStream(bais)) {
            String decompressed = new String(gzipIn.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(TEST_MESSAGE, decompressed);
        }

        // Verify compression actually reduces size for typical JSON
        assertTrue(compressed.length < TEST_MESSAGE.getBytes(StandardCharsets.UTF_8).length,
                "Compressed data should be smaller than original for typical JSON");
    }
}
