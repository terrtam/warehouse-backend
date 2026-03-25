package com.example.warehouse.controller;

import com.example.warehouse.service.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ReportingController {

    @Autowired
    private ReportingService reportingService;

    @GetMapping("/sales-by-product")
    public ResponseEntity<List<Map<String, Object>>> salesByProduct(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportingService.salesByProduct(from, to));
    }

    @GetMapping("/sales-by-category")
    public ResponseEntity<List<Map<String, Object>>> salesByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportingService.salesByCategory(from, to));
    }

    @GetMapping("/purchase-cost-tracking")
    public ResponseEntity<List<Map<String, Object>>> purchaseCostTracking(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportingService.purchaseCostTracking(from, to));
    }

    @GetMapping("/supplier-performance")
    public ResponseEntity<List<Map<String, Object>>> supplierPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportingService.supplierPerformance(from, to));
    }

    @GetMapping("/velocity")
    public ResponseEntity<List<Map<String, Object>>> velocity(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportingService.velocity(from, to));
    }

    @GetMapping("/low-stock-trends")
    public ResponseEntity<List<Map<String, Object>>> lowStockTrends() {
        return ResponseEntity.ok(reportingService.lowStockTrends());
    }
}
