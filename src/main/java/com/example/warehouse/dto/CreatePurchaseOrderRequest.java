package com.example.warehouse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CreatePurchaseOrderRequest {

    @NotNull
    private UUID supplierId;

    @Valid
    @NotEmpty
    private List<CreatePurchaseOrderLineRequest> lines;

    private LocalDate expectedDeliveryDate;

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID supplierId) {
        this.supplierId = supplierId;
    }

    public List<CreatePurchaseOrderLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<CreatePurchaseOrderLineRequest> lines) {
        this.lines = lines;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }
}
