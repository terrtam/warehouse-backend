package com.example.warehouse.service;

import com.example.warehouse.entity.CommunicationChannel;
import com.example.warehouse.entity.CommunicationLogEntity;
import com.example.warehouse.entity.CommunicationOutboxEntity;
import com.example.warehouse.entity.CommunicationStatus;
import com.example.warehouse.event.CommunicationChangedEvent;
import com.example.warehouse.repository.CommunicationLogRepository;
import com.example.warehouse.repository.CommunicationOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunicationDispatcherTest {

    @Mock
    private CommunicationOutboxRepository communicationOutboxRepository;

    @Mock
    private CommunicationLogRepository communicationLogRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private EmailRecipientPolicy emailRecipientPolicy;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommunicationDispatcher communicationDispatcher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(communicationDispatcher, "maxAttempts", 3);
        ReflectionTestUtils.setField(communicationDispatcher, "retryBaseSeconds", 10L);
        when(emailRecipientPolicy.shouldSuppress(anyString())).thenReturn(false);
    }

    @Test
    void dispatchPendingMarksSentAndPublishesEvent() {
        CommunicationOutboxEntity message = pendingMessage();
        when(communicationOutboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                any(), any())).thenReturn(List.of(message));

        communicationDispatcher.dispatchPending();

        assertEquals(CommunicationStatus.SENT, message.getStatus());
        assertEquals(1, message.getAttemptCount());
        assertNull(message.getLastError());

        verify(emailSender).send(message.getRecipient(), message.getSubject(), message.getBody(), message.getHtmlBody());
        verify(communicationOutboxRepository).save(message);
        verify(communicationLogRepository).save(any());

        ArgumentCaptor<CommunicationChangedEvent> eventCaptor = ArgumentCaptor.forClass(CommunicationChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("COMMUNICATION_SENT", eventCaptor.getValue().getEventType());
        assertEquals(message.getId(), eventCaptor.getValue().getEntityId());
    }

    @Test
    void dispatchPendingSchedulesRetryForTransientFailures() {
        CommunicationOutboxEntity message = pendingMessage();
        message.setAttemptCount(1);
        when(communicationOutboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                any(), any())).thenReturn(List.of(message));
        doThrow(new RuntimeException("smtp unavailable")).when(emailSender)
                .send(anyString(), anyString(), anyString(), anyString());

        communicationDispatcher.dispatchPending();

        assertEquals(CommunicationStatus.PENDING, message.getStatus());
        assertEquals(2, message.getAttemptCount());
        assertNotNull(message.getNextAttemptAt());
        assertTrue(message.getNextAttemptAt().isAfter(Instant.now().minusSeconds(1)));
        assertTrue(message.getLastError().contains("smtp unavailable"));

        ArgumentCaptor<CommunicationChangedEvent> eventCaptor = ArgumentCaptor.forClass(CommunicationChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("COMMUNICATION_RETRY_SCHEDULED", eventCaptor.getValue().getEventType());
    }

    @Test
    void dispatchPendingMarksFailedAfterMaxAttempts() {
        CommunicationOutboxEntity message = pendingMessage();
        message.setAttemptCount(2);
        when(communicationOutboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                any(), any())).thenReturn(List.of(message));
        doThrow(new RuntimeException("hard failure")).when(emailSender)
                .send(anyString(), anyString(), anyString(), anyString());

        communicationDispatcher.dispatchPending();

        assertEquals(CommunicationStatus.FAILED, message.getStatus());
        assertEquals(3, message.getAttemptCount());
        assertTrue(message.getLastError().contains("hard failure"));

        ArgumentCaptor<CommunicationChangedEvent> eventCaptor = ArgumentCaptor.forClass(CommunicationChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("COMMUNICATION_FAILED", eventCaptor.getValue().getEventType());
    }

    @Test
    void dispatchPendingSkipsDuplicateProcessingWhenMessageAlreadyUpdatedInLoop() {
        CommunicationOutboxEntity message = pendingMessage();
        when(communicationOutboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                any(), any())).thenReturn(List.of(message, message));

        communicationDispatcher.dispatchPending();

        verify(emailSender, times(1)).send(message.getRecipient(), message.getSubject(), message.getBody(), message.getHtmlBody());
    }

    @Test
    void dispatchPendingSuppressesBlockedRecipients() {
        CommunicationOutboxEntity message = pendingMessage();
        message.setRecipient("ops@seed.test");
        when(communicationOutboxRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                any(), any())).thenReturn(List.of(message));
        when(emailRecipientPolicy.shouldSuppress(message.getRecipient())).thenReturn(true);

        communicationDispatcher.dispatchPending();

        assertEquals(CommunicationStatus.SENT, message.getStatus());
        assertEquals(1, message.getAttemptCount());
        assertNull(message.getLastError());

        verify(emailSender, never()).send(anyString(), anyString(), anyString(), anyString());

        ArgumentCaptor<CommunicationLogEntity> logCaptor = ArgumentCaptor.forClass(CommunicationLogEntity.class);
        verify(communicationLogRepository).save(logCaptor.capture());
        assertTrue(logCaptor.getValue().getDetails().contains("Suppressed fake recipient policy"));

        ArgumentCaptor<CommunicationChangedEvent> eventCaptor = ArgumentCaptor.forClass(CommunicationChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals("COMMUNICATION_SUPPRESSED", eventCaptor.getValue().getEventType());
    }

    private CommunicationOutboxEntity pendingMessage() {
        CommunicationOutboxEntity entity = new CommunicationOutboxEntity();
        entity.setId(UUID.randomUUID());
        entity.setDocumentType("SALES_ORDER");
        entity.setDocumentId(UUID.randomUUID());
        entity.setRecipient("ops@example.com");
        entity.setChannel(CommunicationChannel.EMAIL);
        entity.setSubject("Subject");
        entity.setBody("Body");
        entity.setHtmlBody("<p>Body</p>");
        entity.setStatus(CommunicationStatus.PENDING);
        entity.setAttemptCount(0);
        entity.setNextAttemptAt(Instant.now().minusSeconds(5));
        entity.setCreatedByUsername("tester");
        return entity;
    }
}
