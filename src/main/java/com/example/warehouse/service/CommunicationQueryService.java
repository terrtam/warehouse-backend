package com.example.warehouse.service;

import com.example.warehouse.dto.CommunicationLogDto;
import com.example.warehouse.entity.CommunicationChannel;
import com.example.warehouse.entity.CommunicationLogEntity;
import com.example.warehouse.entity.CommunicationStatus;
import com.example.warehouse.exception.ValidationException;
import com.example.warehouse.repository.CommunicationLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CommunicationQueryService {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;

    @Autowired
    private CommunicationLogRepository communicationLogRepository;

    @Transactional(readOnly = true)
    public List<CommunicationLogDto> listRecent() {
        return listFiltered(null, null, null, DEFAULT_LIMIT);
    }

    @Transactional(readOnly = true)
    public List<CommunicationLogDto> listFiltered(
            String documentType,
            String channel,
            String status,
            Integer limit
    ) {
        String normalizedDocumentType = normalize(documentType);
        CommunicationChannel parsedChannel = parseChannel(channel);
        CommunicationStatus parsedStatus = parseStatus(status);
        int safeLimit = sanitizeLimit(limit);

        Specification<CommunicationLogEntity> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.conjunction();
        if (normalizedDocumentType != null) {
            String uppercaseDocumentType = normalizedDocumentType.toUpperCase(Locale.ENGLISH);
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(
                            criteriaBuilder.upper(root.get("documentType")),
                            uppercaseDocumentType
                    )
            );
        }
        if (parsedChannel != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("channel"), parsedChannel)
            );
        }
        if (parsedStatus != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), parsedStatus)
            );
        }

        return communicationLogRepository.findAll(
                        specification,
                        PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).getContent()
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

    private CommunicationChannel parseChannel(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return CommunicationChannel.valueOf(normalized.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid communication channel");
        }
    }

    private CommunicationStatus parseStatus(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return CommunicationStatus.valueOf(normalized.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid communication status");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CommunicationLogDto toDto(CommunicationLogEntity entity) {
        CommunicationLogDto dto = new CommunicationLogDto();
        dto.setId(entity.getId());
        dto.setDocumentType(entity.getDocumentType());
        dto.setDocumentId(entity.getDocumentId());
        dto.setRecipient(entity.getRecipient());
        dto.setChannel(entity.getChannel() == null ? null : entity.getChannel().name());
        dto.setStatus(entity.getStatus() == null ? null : entity.getStatus().name());
        dto.setSenderUsername(entity.getSenderUsername());
        dto.setDetails(entity.getDetails());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
