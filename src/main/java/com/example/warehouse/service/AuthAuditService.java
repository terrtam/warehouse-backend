package com.example.warehouse.service;

import com.example.warehouse.entity.AuthAuditLogEntity;
import com.example.warehouse.repository.AuthAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthAuditService {

    @Autowired
    private AuthAuditLogRepository authAuditLogRepository;

    @Transactional
    public void logLogin(String username, boolean success, String details) {
        AuthAuditLogEntity entity = new AuthAuditLogEntity();
        entity.setUsername(username);
        entity.setEventType("LOGIN");
        entity.setSuccess(success);
        entity.setDetails(details);
        authAuditLogRepository.save(entity);
    }
}
