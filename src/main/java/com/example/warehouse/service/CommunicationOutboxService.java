package com.example.warehouse.service;

import com.example.warehouse.entity.CommunicationChannel;
import com.example.warehouse.entity.CommunicationOutboxEntity;
import com.example.warehouse.entity.CommunicationStatus;
import com.example.warehouse.event.CommunicationChangedEvent;
import com.example.warehouse.repository.CommunicationOutboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CommunicationOutboxService {

    @Autowired
    private CommunicationOutboxRepository communicationOutboxRepository;

    @Autowired
    private ActorService actorService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public void enqueue(
            String documentType,
            UUID documentId,
            String recipient,
            String subject,
            String textBody,
            String htmlBody
    ) {
        ActorService.Actor actor = actorService.getCurrentActor();

        CommunicationOutboxEntity outbox = new CommunicationOutboxEntity();
        outbox.setDocumentType(documentType);
        outbox.setDocumentId(documentId);
        outbox.setRecipient(recipient);
        outbox.setChannel(CommunicationChannel.EMAIL);
        outbox.setSubject(subject);
        outbox.setBody(textBody);
        outbox.setHtmlBody(htmlBody);
        outbox.setStatus(CommunicationStatus.PENDING);
        outbox.setCreatedBy(actor.getUserId());
        outbox.setCreatedByUsername(actor.getUsername());
        CommunicationOutboxEntity saved = communicationOutboxRepository.save(outbox);
        eventPublisher.publishEvent(CommunicationChangedEvent.of("COMMUNICATION_QUEUED", saved.getId(), 0L));
    }
}
