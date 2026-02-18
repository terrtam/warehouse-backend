package com.example.warehouse.event;

import com.example.warehouse.dto.CustomerDto;

import java.time.Instant;

public class CustomerChangedEvent {

    private final String eventType;
    private final CustomerDto customer;
    private final Instant occurredAt;

    public CustomerChangedEvent(String eventType, CustomerDto customer, Instant occurredAt) {
        this.eventType = eventType;
        this.customer = customer;
        this.occurredAt = occurredAt;
    }

    public static CustomerChangedEvent created(CustomerDto customer) {
        return new CustomerChangedEvent("CUSTOMER_CREATED", customer, Instant.now());
    }

    public static CustomerChangedEvent updated(CustomerDto customer) {
        return new CustomerChangedEvent("CUSTOMER_UPDATED", customer, Instant.now());
    }

    public String getEventType() {
        return eventType;
    }

    public CustomerDto getCustomer() {
        return customer;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
