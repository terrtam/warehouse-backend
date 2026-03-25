package com.example.warehouse.realtime;

import com.example.warehouse.event.CategoryChangedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CategoryChangedRealtimePublisher {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryChanged(CategoryChangedEvent event) {
        messagingTemplate.convertAndSend("/topic/categories", event);
        messagingTemplate.convertAndSend("/topic/products", event);
    }
}
