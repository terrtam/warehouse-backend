package com.example.warehouse.event;

import java.time.Instant;
import java.util.UUID;

public class CategoryChangedEvent {

    private final String eventType;
    private final UUID entityId;
    private final Long version;
    private final Instant occurredAt;

    public CategoryChangedEvent(String eventType, UUID entityId, Long version, Instant occurredAt) {
        this.eventType = eventType;
        this.entityId = entityId;
        this.version = version;
        this.occurredAt = occurredAt;
    }

    public static CategoryChangedEvent created(UUID entityId, Long version) {
        return new CategoryChangedEvent("CATEGORY_CREATED", entityId, version, Instant.now());
    }

    public static CategoryChangedEvent updated(UUID entityId, Long version) {
        return new CategoryChangedEvent("CATEGORY_UPDATED", entityId, version, Instant.now());
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
