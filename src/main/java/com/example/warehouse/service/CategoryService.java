package com.example.warehouse.service;

import com.example.warehouse.dto.CategoryDto;
import com.example.warehouse.dto.CreateCategoryRequest;
import com.example.warehouse.dto.UpdateCategoryRequest;
import com.example.warehouse.entity.CategoryEntity;
import com.example.warehouse.event.CategoryChangedEvent;
import com.example.warehouse.exception.ConflictException;
import com.example.warehouse.exception.NotFoundException;
import com.example.warehouse.exception.ValidationException;
import com.example.warehouse.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private EntityAuditService entityAuditService;

    @Transactional(readOnly = true)
    public List<CategoryDto> listCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CategoryDto createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("Category name already exists");
        }

        CategoryEntity entity = new CategoryEntity();
        apply(entity, request.getName(), request.getDescription(), request.getStatus());
        CategoryEntity saved = categoryRepository.saveAndFlush(entity);
        entityAuditService.log("CATEGORY", saved.getId(), "CREATE", null, saved.getStatus());
        eventPublisher.publishEvent(CategoryChangedEvent.created(saved.getId(), saved.getVersion()));
        return toDto(saved);
    }

    @Transactional
    public CategoryDto updateCategory(UUID id, UpdateCategoryRequest request) {
        CategoryEntity entity = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        if (!Objects.equals(entity.getVersion(), request.getVersion())) {
            throw new ConflictException("Category version conflict");
        }

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new ConflictException("Category name already exists");
        }

        apply(entity, request.getName(), request.getDescription(), request.getStatus());
        CategoryEntity saved = categoryRepository.saveAndFlush(entity);
        entityAuditService.log("CATEGORY", saved.getId(), "UPDATE", null, saved.getStatus());
        eventPublisher.publishEvent(CategoryChangedEvent.updated(saved.getId(), saved.getVersion()));
        return toDto(saved);
    }

    private void apply(CategoryEntity entity, String name, String description, String status) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty()) {
            throw new ValidationException("Category name is required");
        }
        entity.setName(normalizedName);
        entity.setDescription(normalizeNullable(description));
        entity.setStatus(normalizeStatus(status));
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return CategoryEntity.DEFAULT_STATUS;
        }
        return value.trim().toUpperCase(Locale.ENGLISH);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CategoryDto toDto(CategoryEntity category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setStatus(category.getStatus());
        dto.setVersion(category.getVersion());
        dto.setCreatedAt(category.getCreatedAt());
        dto.setUpdatedAt(category.getUpdatedAt());
        return dto;
    }
}
