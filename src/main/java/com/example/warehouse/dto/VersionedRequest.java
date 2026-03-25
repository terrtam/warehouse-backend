package com.example.warehouse.dto;

import jakarta.validation.constraints.NotNull;

public class VersionedRequest {

    @NotNull
    private Long version;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
