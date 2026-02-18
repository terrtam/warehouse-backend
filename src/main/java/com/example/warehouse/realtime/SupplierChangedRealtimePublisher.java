package com.example.warehouse.realtime;

import com.example.warehouse.event.SupplierChangedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SupplierChangedRealtimePublisher {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSupplierChanged(SupplierChangedEvent event) {
        messagingTemplate.convertAndSend("/topic/suppliers", event);
    }
}
