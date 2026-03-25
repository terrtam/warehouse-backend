package com.example.warehouse.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class ShipSalesOrderRequest {

    @NotNull
    private Long version;

    @Valid
    @NotEmpty
    private List<ShipSalesOrderLineRequest> lines;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public List<ShipSalesOrderLineRequest> getLines() {
        return lines;
    }

    public void setLines(List<ShipSalesOrderLineRequest> lines) {
        this.lines = lines;
    }
}
