package com.example.warehouse.service;

import com.example.warehouse.entity.EntityAuditLogEntity;
import com.example.warehouse.repository.EntityAuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EntityAuditService {

    @Autowired
    private EntityAuditLogRepository entityAuditLogRepository;

    @Autowired
    private ActorService actorService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void log(String entityType, UUID entityId, String action, Object oldValue, Object newValue) {
        ActorService.Actor actor = actorService.getCurrentActor();
        EntityAuditLogEntity log = new EntityAuditLogEntity();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setOldValue(toJson(oldValue));
        log.setNewValue(toJson(newValue));
        log.setPerformedBy(actor.getUserId());
        log.setPerformedByUsername(actor.getUsername());
        entityAuditLogRepository.save(log);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"serializationError\":\"" + ex.getMessage() + "\"}";
        }
    }
}
