package com.example.warehouse.service;

import com.example.warehouse.dto.EntityAuditLogDto;
import com.example.warehouse.entity.EntityAuditLogEntity;
import com.example.warehouse.exception.ValidationException;
import com.example.warehouse.repository.EntityAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditLogQueryService {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;

    @Autowired
    private EntityAuditLogRepository entityAuditLogRepository;

    @Transactional(readOnly = true)
    public List<EntityAuditLogDto> listRecent() {
        return listFiltered(null, null, null, DEFAULT_LIMIT);
    }

    @Transactional(readOnly = true)
    public List<EntityAuditLogDto> listFiltered(
            String entityType,
            String action,
            UUID entityId,
            Integer limit
    ) {
        int safeLimit = sanitizeLimit(limit);
        return entityAuditLogRepository.findFiltered(
                        normalize(entityType),
                        normalize(action),
                        entityId,
                        PageRequest.of(0, safeLimit)
                )
                .stream()
                .map(this::toDto)
                .toList();
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1) {
            throw new ValidationException("limit must be at least 1");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private EntityAuditLogDto toDto(EntityAuditLogEntity entity) {
        EntityAuditLogDto dto = new EntityAuditLogDto();
        dto.setId(entity.getId());
        dto.setEntityType(entity.getEntityType());
        dto.setEntityId(entity.getEntityId());
        dto.setAction(entity.getAction());
        dto.setOldValue(entity.getOldValue());
        dto.setNewValue(entity.getNewValue());
        dto.setPerformedBy(entity.getPerformedBy());
        dto.setPerformedByUsername(entity.getPerformedByUsername());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
