package com.example.warehouse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ReceivePurchaseOrderRequest {

    @NotNull
    private Long version;

    @Valid
    @NotEmpty
    private List<ReceivePurchaseOrderLineRequest> lines;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<ReceivePurchaseOrderLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<ReceivePurchaseOrderLineRequest> lines) {
        this.lines = lines;
    }
}
