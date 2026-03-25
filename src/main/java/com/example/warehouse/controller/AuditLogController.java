package com.example.warehouse.controller;

import com.example.warehouse.dto.EntityAuditLogDto;
import com.example.warehouse.service.AuditLogQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-log")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AuditLogController {

    @Autowired
    private AuditLogQueryService auditLogQueryService;

    @GetMapping
    public ResponseEntity<List<EntityAuditLogDto>> listRecent(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(auditLogQueryService.listFiltered(entityType, action, entityId, limit));
    }
}
