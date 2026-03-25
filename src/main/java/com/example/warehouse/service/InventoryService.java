package com.example.warehouse.service;

import com.example.warehouse.dto.CreateInventoryAdjustmentRequest;
import com.example.warehouse.dto.InventoryRecordDto;
import com.example.warehouse.dto.InventoryTransactionDto;
import com.example.warehouse.entity.InventoryTransactionEntity;
import com.example.warehouse.entity.InventoryTransactionType;
import com.example.warehouse.entity.Product;
import com.example.warehouse.event.InventoryChangedEvent;
import com.example.warehouse.exception.NotFoundException;
import com.example.warehouse.exception.ValidationException;
import com.example.warehouse.repository.InventoryTransactionRepository;
import com.example.warehouse.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private InventoryComputationService inventoryComputationService;

    @Autowired
    private ActorService actorService;

    @Autowired
    private EntityAuditService entityAuditService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<InventoryRecordDto> listInventory() {
        List<Product> products = productRepository.findAll();
        Map<UUID, Integer> onHandByProduct = computeOnHandByProduct();

        return products.stream()
                .map(product -> toInventoryRecord(product, onHandByProduct.getOrDefault(product.getId(), 0)))
                .sorted(java.util.Comparator.comparing(InventoryRecordDto::getProductName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDto> listTransactions() {
        List<InventoryTransactionEntity> transactions = inventoryTransactionRepository.findAllByOrderByCreatedAtDesc();
        Set<UUID> productIds = transactions.stream()
                .map(InventoryTransactionEntity::getProductId)
                .collect(Collectors.toSet());
        Map<UUID, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return transactions.stream()
                .map(txn -> toTransactionDto(txn, productMap.get(txn.getProductId())))
                .toList();
    }

    @Transactional
    public InventoryRecordDto createAdjustment(CreateInventoryAdjustmentRequest request) {
        if (request.getQuantityDelta() == null || request.getQuantityDelta() == 0) {
            throw new ValidationException("Adjustment quantity cannot be zero");
        }
        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new ValidationException("Adjustment reason is required");
        }

        Product product = productRepository.findByIdForUpdate(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found"));

        int onHand = inventoryComputationService.onHandForProduct(product.getId());
        int nextQuantity = onHand + request.getQuantityDelta();
        if (nextQuantity < 0 && !(request.isAllowNegativeOverride() && isManager())) {
            throw new ValidationException("Inventory cannot go negative without manager override");
        }

        ActorService.Actor actor = actorService.getCurrentActor();
        InventoryTransactionEntity txn = new InventoryTransactionEntity();
        txn.setProductId(product.getId());
        txn.setTransactionType(InventoryTransactionType.ADJUST);
        txn.setQuantity(request.getQuantityDelta());
        txn.setReferenceType("ADJUSTMENT");
        txn.setReason(request.getReason().trim());
        txn.setUnitPrice(request.getUnitPrice());
        txn.setPerformedBy(actor.getUserId());
        txn.setCreatedByUsername(actor.getUsername());
        InventoryTransactionEntity saved = inventoryTransactionRepository.saveAndFlush(txn);

        entityAuditService.log("INVENTORY_TRANSACTION", saved.getId(), "CREATE", null, saved.getTransactionType().name());
        eventPublisher.publishEvent(InventoryChangedEvent.of("INVENTORY_ADJUSTED", product.getId(), saved.getVersion()));

        return toInventoryRecord(product, nextQuantity);
    }

    private boolean isManager() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(authority -> "ROLE_MANAGER".equals(authority.getAuthority()));
    }

    private Map<UUID, Integer> computeOnHandByProduct() {
        Map<UUID, Integer> onHandByProduct = new HashMap<>();
        for (InventoryTransactionEntity txn : inventoryTransactionRepository.findAll()) {
            int delta = toSignedQuantity(txn);
            onHandByProduct.merge(txn.getProductId(), delta, Integer::sum);
        }
        return onHandByProduct;
    }

    private int toSignedQuantity(InventoryTransactionEntity txn) {
        if (txn.getTransactionType() == InventoryTransactionType.OUT) {
            return -txn.getQuantity();
        }
        return txn.getQuantity();
    }

    private InventoryRecordDto toInventoryRecord(Product product, int onHand) {
        int reserved = inventoryComputationService.reservedForProduct(product.getId());
        int available = onHand - reserved;

        InventoryRecordDto dto = new InventoryRecordDto();
        dto.setProductId(product.getId());
        dto.setProductName(product.getName());
        dto.setSku(product.getSku());
        dto.setOnHand(onHand);
        dto.setReserved(reserved);
        dto.setAvailable(available);
        dto.setReorderThreshold(product.getReorderThreshold() == null ? 0 : product.getReorderThreshold());
        dto.setLowStock(available <= dto.getReorderThreshold());
        return dto;
    }

    private InventoryTransactionDto toTransactionDto(InventoryTransactionEntity txn, Product product) {
        InventoryTransactionDto dto = new InventoryTransactionDto();
        dto.setId(txn.getId());
        dto.setProductId(txn.getProductId());
        dto.setProductName(product == null ? null : product.getName());
        dto.setSku(product == null ? null : product.getSku());
        dto.setQuantity(txn.getQuantity());
        dto.setType(txn.getTransactionType().name());
        dto.setReferenceType(txn.getReferenceType());
        dto.setReferenceId(txn.getReferenceId());
        dto.setReferenceLineId(txn.getReferenceLineId());
        dto.setUnitPrice(txn.getUnitPrice());
        dto.setReason(txn.getReason());
        dto.setPerformedBy(txn.getPerformedBy());
        dto.setPerformedByUsername(txn.getCreatedByUsername());
        dto.setVersion(txn.getVersion());
        dto.setCreatedAt(txn.getCreatedAt());
        dto.setUpdatedAt(txn.getUpdatedAt());
        return dto;
    }
}
