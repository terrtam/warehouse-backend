package com.example.warehouse.realtime;

import com.example.warehouse.event.CommunicationChangedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CommunicationChangedRealtimePublisher {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommunicationChanged(CommunicationChangedEvent event) {
        messagingTemplate.convertAndSend("/topic/communications", event);
    }
}
