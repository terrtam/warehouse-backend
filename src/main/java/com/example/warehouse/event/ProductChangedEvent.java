package com.example.warehouse.event;

import java.time.Instant;
import java.util.UUID;

public class ProductChangedEvent {

    private final String type;
    private final UUID id;
    private final Long version;
    private final Instant at;

    public ProductChangedEvent(String type, UUID id, Long version, Instant at) {
        this.type = type;
        this.id = id;
        this.version = version;
        this.at = at;
    }

    public static ProductChangedEvent created(UUID id, Long version) {
        return new ProductChangedEvent("product.created", id, version, Instant.now());
    }

    public static ProductChangedEvent updated(UUID id, Long version) {
        return new ProductChangedEvent("product.updated", id, version, Instant.now());
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
