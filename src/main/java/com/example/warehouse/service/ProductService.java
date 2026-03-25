package com.example.warehouse.service;

import com.example.warehouse.dto.CreateProductRequest;
import com.example.warehouse.dto.ProductDto;
import com.example.warehouse.dto.UpdateProductRequest;
import com.example.warehouse.entity.Product;
import com.example.warehouse.event.ProductChangedEvent;
import com.example.warehouse.exception.ConflictException;
import com.example.warehouse.exception.NotFoundException;
import com.example.warehouse.exception.ValidationException;
import com.example.warehouse.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private EntityAuditService entityAuditService;

    @Transactional(readOnly = true)
    public Page<ProductDto> listProducts(String q, String status, UUID categoryId, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();

        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("sku")), like),
                    cb.like(cb.lower(root.get("categoryName")), like)
            ));
        }

        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (categoryId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("categoryId"), categoryId));
        }

        return productRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ProductDto getProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return toDto(product);
    }

    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        validateNonNegative(request);

        if (productRepository.existsBySku(request.getSku())) {
            throw new ValidationException("SKU already exists");
        }

        Product product = new Product();
        applyRequest(product, request);

        Product saved = productRepository.save(product);
        entityAuditService.log("PRODUCT", saved.getId(), "CREATE", null, saved.getStatus());
        eventPublisher.publishEvent(ProductChangedEvent.created(saved.getId(), saved.getVersion()));

        return toDto(saved);
    }

    @Transactional
    public ProductDto updateProduct(UUID id, UpdateProductRequest request) {
        validateNonNegative(request);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        if (!Objects.equals(product.getVersion(), request.getVersion())) {
            throw new ConflictException("Product version conflict");
        }

        if (productRepository.existsBySkuAndIdNot(request.getSku(), id)) {
            throw new ValidationException("SKU already exists");
        }

        applyRequest(product, request);
        Product saved = productRepository.save(product);
        entityAuditService.log("PRODUCT", saved.getId(), "UPDATE", null, saved.getStatus());
        eventPublisher.publishEvent(ProductChangedEvent.updated(saved.getId(), saved.getVersion()));

        return toDto(saved);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));
        String oldStatus = product.getStatus();
        if ("INACTIVE".equalsIgnoreCase(oldStatus)) {
            return;
        }
        product.setStatus("INACTIVE");
        Product saved = productRepository.saveAndFlush(product);
        entityAuditService.log("PRODUCT", saved.getId(), "DEACTIVATE", oldStatus, saved.getStatus());
        eventPublisher.publishEvent(ProductChangedEvent.updated(saved.getId(), saved.getVersion()));
    }

    private void applyRequest(Product product, CreateProductRequest request) {
        applyRequest(
                product,
                request.getName(),
                request.getSku(),
                request.getCategoryId(),
                request.getCategoryName(),
                request.getDescription(),
                request.getUnit(),
                request.getDefaultSalePrice(),
                request.getCostPrice(),
                request.getReorderThreshold(),
                request.getStatus()
        );
    }

    private void applyRequest(Product product, UpdateProductRequest request) {
        applyRequest(
                product,
                request.getName(),
                request.getSku(),
                request.getCategoryId(),
                request.getCategoryName(),
                request.getDescription(),
                request.getUnit(),
                request.getDefaultSalePrice(),
                request.getCostPrice(),
                request.getReorderThreshold(),
                request.getStatus()
        );
    }

    private void applyRequest(
            Product product,
            String name,
            String sku,
            UUID categoryId,
            String categoryName,
            String description,
            String unit,
            BigDecimal defaultSalePrice,
            BigDecimal costPrice,
            Integer reorderThreshold,
            String status
    ) {
        product.setName(name);
        product.setSku(sku);
        product.setCategoryId(categoryId);
        product.setCategoryName(categoryName);
        product.setDescription(description);
        product.setUnit(unit);
        product.setDefaultSalePrice(defaultSalePrice);
        product.setCostPrice(costPrice);
        product.setReorderThreshold(reorderThreshold);
        product.setStatus(normalizeStatus(status));
    }

    private ProductDto toDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSku(product.getSku());
        dto.setCategoryId(product.getCategoryId());
        dto.setCategoryName(product.getCategoryName());
        dto.setDescription(product.getDescription());
        dto.setUnit(product.getUnit());
        dto.setDefaultSalePrice(product.getDefaultSalePrice());
        dto.setCostPrice(product.getCostPrice());
        dto.setReorderThreshold(product.getReorderThreshold());
        dto.setStatus(product.getStatus());
        dto.setVersion(product.getVersion());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        return dto;
    }

    private void validateNonNegative(CreateProductRequest request) {
        validateNonNegative(request.getDefaultSalePrice(), "defaultSalePrice");
        validateNonNegative(request.getCostPrice(), "costPrice");
        validateNonNegative(request.getReorderThreshold(), "reorderThreshold");
    }

    private void validateNonNegative(UpdateProductRequest request) {
        validateNonNegative(request.getDefaultSalePrice(), "defaultSalePrice");
        validateNonNegative(request.getCostPrice(), "costPrice");
        validateNonNegative(request.getReorderThreshold(), "reorderThreshold");
    }

    private void validateNonNegative(BigDecimal value, String field) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException(field + " must be non-negative");
        }
    }

    private void validateNonNegative(Integer value, String field) {
        if (value != null && value < 0) {
            throw new ValidationException(field + " must be non-negative");
        }
    }

    private String normalizeStatus(String value) {
        if (value == null || value.isBlank()) {
            return "ACTIVE";
        }
        return value.trim().toUpperCase(Locale.ENGLISH);
    }
}
