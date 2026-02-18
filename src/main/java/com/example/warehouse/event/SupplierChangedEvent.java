package com.example.warehouse.event;

import java.time.Instant;
import java.util.UUID;

public class SupplierChangedEvent {

    private final String type;
    private final UUID id;
    private final Long version;
    private final Instant at;

    public SupplierChangedEvent(String type, UUID id, Long version, Instant at) {
        this.type = type;
        this.id = id;
        this.version = version;
        this.at = at;
    }

    public static SupplierChangedEvent created(UUID id, Long version) {
        return new SupplierChangedEvent("supplier.created", id, version, Instant.now());
    }

    public static SupplierChangedEvent updated(UUID id, Long version) {
        return new SupplierChangedEvent("supplier.updated", id, version, Instant.now());
    }

    public String getType() {
        return type;
    }

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getAt() {
        return at;
    }
}
