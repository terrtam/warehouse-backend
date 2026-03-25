package com.example.warehouse.controller;

import com.example.warehouse.dto.CreateInventoryAdjustmentRequest;
import com.example.warehouse.dto.InventoryRecordDto;
import com.example.warehouse.dto.InventoryTransactionDto;
import com.example.warehouse.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryRecordDto>> listInventory() {
        return ResponseEntity.ok(inventoryService.listInventory());
    }

    @PostMapping("/adjustments")
    public ResponseEntity<InventoryRecordDto> createAdjustment(
            @Valid @RequestBody CreateInventoryAdjustmentRequest request
    ) {
        return ResponseEntity.ok(inventoryService.createAdjustment(request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<InventoryTransactionDto>> listTransactions() {
        return ResponseEntity.ok(inventoryService.listTransactions());
    }
}
