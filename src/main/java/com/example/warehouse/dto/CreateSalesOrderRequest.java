package com.example.warehouse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CreateSalesOrderRequest {

    @NotNull
    private UUID customerId;

    @Valid
    @NotEmpty
    private List<CreateSalesOrderLineRequest> lines;

    private LocalDate expectedDeliveryDate;

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public List<CreateSalesOrderLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<CreateSalesOrderLineRequest> lines) {
        this.lines = lines;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }
}
