package com.example.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class CreateInventoryAdjustmentRequest {

    @NotNull
    private UUID productId;

    @NotNull
    private Integer quantityDelta;

    @NotBlank
    private String reason;

    private boolean allowNegativeOverride;

    private BigDecimal unitPrice;

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public Integer getQuantityDelta() {
        return quantityDelta;
    }

    public void setQuantityDelta(Integer quantityDelta) {
        this.quantityDelta = quantityDelta;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isAllowNegativeOverride() {
        return allowNegativeOverride;
    }

    public void setAllowNegativeOverride(boolean allowNegativeOverride) {
        this.allowNegativeOverride = allowNegativeOverride;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
