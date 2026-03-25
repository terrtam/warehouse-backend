package com.example.warehouse.controller;

import com.example.warehouse.dto.CreateSalesOrderRequest;
import com.example.warehouse.dto.SalesOrderDto;
import com.example.warehouse.dto.ShipSalesOrderRequest;
import com.example.warehouse.dto.VersionedRequest;
import com.example.warehouse.service.SalesOrderService;
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
@RequestMapping("/api/sales-orders")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class SalesOrderController {

    @Autowired
    private SalesOrderService salesOrderService;

    @GetMapping
    public ResponseEntity<List<SalesOrderDto>> listOrders() {
        return ResponseEntity.ok(salesOrderService.listOrders());
    }

    @PostMapping
    public ResponseEntity<SalesOrderDto> createOrder(@Valid @RequestBody CreateSalesOrderRequest request) {
        SalesOrderDto created = salesOrderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<SalesOrderDto> confirmOrder(
            @PathVariable UUID id,
            @Valid @RequestBody VersionedRequest request
    ) {
        return ResponseEntity.ok(salesOrderService.confirmOrder(id, request.getVersion()));
    }

    @PostMapping("/{id}/ship")
    public ResponseEntity<SalesOrderDto> shipOrder(
            @PathVariable UUID id,
            @Valid @RequestBody ShipSalesOrderRequest request
    ) {
        return ResponseEntity.ok(salesOrderService.shipOrder(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SalesOrderDto> cancelOrder(
            @PathVariable UUID id,
            @Valid @RequestBody VersionedRequest request
    ) {
        return ResponseEntity.ok(salesOrderService.cancelOrder(id, request.getVersion()));
    }
}
