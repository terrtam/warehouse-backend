package com.example.warehouse.controller;

import com.example.warehouse.dto.CreatePurchaseOrderRequest;
import com.example.warehouse.dto.PurchaseOrderDto;
import com.example.warehouse.dto.ReceivePurchaseOrderRequest;
import com.example.warehouse.dto.VersionedRequest;
import com.example.warehouse.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/purchase-orders")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ResponseEntity<List<PurchaseOrderDto>> listOrders() {
        return ResponseEntity.ok(purchaseOrderService.listOrders());
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderDto> createOrder(@Valid @RequestBody CreatePurchaseOrderRequest request) {
        PurchaseOrderDto created = purchaseOrderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/order")
    public ResponseEntity<PurchaseOrderDto> markOrdered(
            @PathVariable UUID id,
            @Valid @RequestBody VersionedRequest request
    ) {
        return ResponseEntity.ok(purchaseOrderService.markOrdered(id, request.getVersion()));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<PurchaseOrderDto> receiveOrder(
            @PathVariable UUID id,
            @Valid @RequestBody ReceivePurchaseOrderRequest request
    ) {
        return ResponseEntity.ok(purchaseOrderService.receiveOrder(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrderDto> cancelOrder(
            @PathVariable UUID id,
            @Valid @RequestBody VersionedRequest request
    ) {
        return ResponseEntity.ok(purchaseOrderService.cancelOrder(id, request.getVersion()));
    }
}
