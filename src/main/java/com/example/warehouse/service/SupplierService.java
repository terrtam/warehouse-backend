package com.example.warehouse.service;

import com.example.warehouse.dto.CreateSupplierRequest;
import com.example.warehouse.dto.SupplierDto;
import com.example.warehouse.dto.UpdateSupplierRequest;
import com.example.warehouse.entity.SupplierEntity;
import com.example.warehouse.event.SupplierChangedEvent;
import com.example.warehouse.exception.ConflictException;
import com.example.warehouse.exception.NotFoundException;
import com.example.warehouse.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public SupplierDto getSupplier(UUID id) {
        SupplierEntity supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier not found"));
        return toDto(supplier);
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto> listSuppliers(Instant updatedAfter, Pageable pageable) {
        Page<SupplierEntity> page = updatedAfter == null
                ? supplierRepository.findAll(pageable)
                : supplierRepository.findAllByUpdatedAtGreaterThan(updatedAfter, pageable);
        return page.map(this::toDto);
    }

    @Transactional
    public SupplierDto createSupplier(CreateSupplierRequest request) {
        SupplierEntity supplier = new SupplierEntity();
        applyCreateRequest(supplier, request);

        SupplierEntity saved = supplierRepository.saveAndFlush(supplier);
        eventPublisher.publishEvent(SupplierChangedEvent.created(saved.getId(), saved.getVersion()));
        return toDto(saved);
    }

    @Transactional
    public SupplierDto updateSupplier(UUID id, UpdateSupplierRequest request) {
        SupplierEntity supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Supplier not found"));

        if (!Objects.equals(supplier.getVersion(), request.getVersion())) {
            throw new ConflictException("Supplier version conflict");
        }

        applyUpdateRequest(supplier, request);
        SupplierEntity saved = supplierRepository.saveAndFlush(supplier);
        eventPublisher.publishEvent(SupplierChangedEvent.updated(saved.getId(), saved.getVersion()));
        return toDto(saved);
    }

    private void applyCreateRequest(SupplierEntity supplier, CreateSupplierRequest request) {
        supplier.setName(request.getName());
        supplier.setEmail(normalizeNullable(request.getEmail()));
        supplier.setPhone(normalizeNullable(request.getPhone()));
        supplier.setAddress(normalizeNullable(request.getAddress()));
        supplier.setStatus(normalizeNullable(request.getStatus()));
    }

    private void applyUpdateRequest(SupplierEntity supplier, UpdateSupplierRequest request) {
        supplier.setName(request.getName());
        supplier.setEmail(normalizeNullable(request.getEmail()));
        supplier.setPhone(normalizeNullable(request.getPhone()));
        supplier.setAddress(normalizeNullable(request.getAddress()));
        supplier.setStatus(normalizeNullable(request.getStatus()));
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SupplierDto toDto(SupplierEntity supplier) {
        SupplierDto dto = new SupplierDto();
        dto.setId(supplier.getId());
        dto.setName(supplier.getName());
        dto.setEmail(supplier.getEmail());
        dto.setPhone(supplier.getPhone());
        dto.setAddress(supplier.getAddress());
        dto.setStatus(supplier.getStatus());
        dto.setVersion(supplier.getVersion());
        dto.setCreatedAt(supplier.getCreatedAt());
        dto.setUpdatedAt(supplier.getUpdatedAt());
        return dto;
    }
}
