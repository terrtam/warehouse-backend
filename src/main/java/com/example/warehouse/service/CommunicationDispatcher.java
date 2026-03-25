package com.example.warehouse.service;

import com.example.warehouse.entity.CommunicationLogEntity;
import com.example.warehouse.entity.CommunicationOutboxEntity;
import com.example.warehouse.entity.CommunicationStatus;
import com.example.warehouse.event.CommunicationChangedEvent;
import com.example.warehouse.repository.CommunicationLogRepository;
import com.example.warehouse.repository.CommunicationOutboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class CommunicationDispatcher {

    @Autowired
    private CommunicationOutboxRepository communicationOutboxRepository;

    @Autowired
    private CommunicationLogRepository communicationLogRepository;

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private EmailRecipientPolicy emailRecipientPolicy;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Value("${wms.communication.max-attempts:5}")
    private int maxAttempts;

    @Value("${wms.communication.retry-base-seconds:30}")
    private long retryBaseSeconds;

    @Scheduled(fixedDelayString = "${wms.communication.dispatch-interval-ms:10000}")
    @Transactional
    public void dispatchPending() {
        Instant now = Instant.now();
        List<CommunicationOutboxEntity> pending = communicationOutboxRepository
                .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        CommunicationStatus.PENDING,
                        now
                );

        for (CommunicationOutboxEntity message : pending) {
            if (message.getStatus() != CommunicationStatus.PENDING) {
                continue;
            }
            if (message.getNextAttemptAt() != null && message.getNextAttemptAt().isAfter(now)) {
                continue;
            }
            dispatchSingle(message, now);
        }
    }

    private void dispatchSingle(CommunicationOutboxEntity message, Instant now) {
        if (emailRecipientPolicy.shouldSuppress(message.getRecipient())) {
            int attempts = incrementAttemptCount(message);
            message.setStatus(CommunicationStatus.SENT);
            message.setLastError(null);
            message.setNextAttemptAt(now);
            communicationOutboxRepository.save(message);

            saveLog(message, CommunicationStatus.SENT, "Suppressed fake recipient policy");
            eventPublisher.publishEvent(
                    CommunicationChangedEvent.of("COMMUNICATION_SUPPRESSED", message.getId(), (long) attempts)
            );
            return;
        }

        try {
            emailSender.send(message.getRecipient(), message.getSubject(), message.getBody(), message.getHtmlBody());
            int attempts = incrementAttemptCount(message);
            message.setStatus(CommunicationStatus.SENT);
            message.setLastError(null);
            message.setNextAttemptAt(now);
            communicationOutboxRepository.save(message);

            saveLog(message, CommunicationStatus.SENT, "Email dispatched");
            eventPublisher.publishEvent(
                    CommunicationChangedEvent.of("COMMUNICATION_SENT", message.getId(), (long) attempts)
            );
        } catch (Exception ex) {
            handleFailure(message, ex, now);
        }
    }

    private void handleFailure(CommunicationOutboxEntity message, Exception ex, Instant now) {
        int attempts = incrementAttemptCount(message);
        String error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        message.setLastError(error);

        if (attempts >= Math.max(1, maxAttempts)) {
            message.setStatus(CommunicationStatus.FAILED);
            message.setNextAttemptAt(now);
            saveLog(message, CommunicationStatus.FAILED, "Delivery failed after " + attempts + " attempts: " + error);
            eventPublisher.publishEvent(
                    CommunicationChangedEvent.of("COMMUNICATION_FAILED", message.getId(), (long) attempts)
            );
        } else {
            long delaySeconds = retryDelaySeconds(attempts);
            Instant nextAttempt = now.plusSeconds(delaySeconds);
            message.setStatus(CommunicationStatus.PENDING);
            message.setNextAttemptAt(nextAttempt);
            saveLog(
                    message,
                    CommunicationStatus.FAILED,
                    "Attempt " + attempts + " failed; retry scheduled at " + nextAttempt + ": " + error
            );
            eventPublisher.publishEvent(
                    CommunicationChangedEvent.of("COMMUNICATION_RETRY_SCHEDULED", message.getId(), (long) attempts)
            );
        }
        communicationOutboxRepository.save(message);
    }

    private int incrementAttemptCount(CommunicationOutboxEntity message) {
        int attempts = message.getAttemptCount() == null ? 0 : message.getAttemptCount();
        attempts += 1;
        message.setAttemptCount(attempts);
        return attempts;
    }

    private long retryDelaySeconds(int attempts) {
        long base = Math.max(1, retryBaseSeconds);
        int exponent = Math.max(0, Math.min(attempts - 1, 10));
        long multiplier = 1L << exponent;
        long delay = base * multiplier;
        return Math.min(delay, 3600L);
    }

    private void saveLog(CommunicationOutboxEntity message, CommunicationStatus status, String details) {
        CommunicationLogEntity log = new CommunicationLogEntity();
        log.setOutboxId(message.getId());
        log.setDocumentType(message.getDocumentType());
        log.setDocumentId(message.getDocumentId());
        log.setRecipient(message.getRecipient());
        log.setChannel(message.getChannel());
        log.setStatus(status);
        log.setSenderUsername(message.getCreatedByUsername());
        log.setDetails(details);
        communicationLogRepository.save(log);
    }
}
