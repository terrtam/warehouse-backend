package com.example.warehouse.controller;

import com.example.warehouse.dto.CommunicationLogDto;
import com.example.warehouse.service.CommunicationQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/communications")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class CommunicationController {

    @Autowired
    private CommunicationQueryService communicationQueryService;

    @GetMapping
    public ResponseEntity<List<CommunicationLogDto>> listRecent(
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(communicationQueryService.listFiltered(documentType, channel, status, limit));
    }
}
