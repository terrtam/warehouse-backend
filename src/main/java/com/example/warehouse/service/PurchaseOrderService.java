package com.example.warehouse.service;

import com.example.warehouse.dto.CreatePurchaseOrderLineRequest;
import com.example.warehouse.dto.CreatePurchaseOrderRequest;
import com.example.warehouse.dto.PurchaseOrderDto;
import com.example.warehouse.dto.PurchaseOrderLineDto;
import com.example.warehouse.dto.ReceivePurchaseOrderLineRequest;
import com.example.warehouse.dto.ReceivePurchaseOrderRequest;
import com.example.warehouse.entity.InventoryTransactionEntity;
import com.example.warehouse.entity.InventoryTransactionType;
import com.example.warehouse.entity.Product;
import com.example.warehouse.entity.PurchaseOrderEntity;
import com.example.warehouse.entity.PurchaseOrderLineEntity;
import com.example.warehouse.entity.PurchaseOrderStatus;
import com.example.warehouse.entity.SupplierEntity;
import com.example.warehouse.event.InventoryChangedEvent;
import com.example.warehouse.event.OrderChangedEvent;
import com.example.warehouse.exception.ConflictException;
import com.example.warehouse.exception.NotFoundException;
import com.example.warehouse.exception.ValidationException;
import com.example.warehouse.repository.InventoryTransactionRepository;
import com.example.warehouse.repository.ProductRepository;
import com.example.warehouse.repository.PurchaseOrderRepository;
import com.example.warehouse.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private ActorService actorService;

    @Autowired
    private EntityAuditService entityAuditService;

    @Autowired
    private CommunicationOutboxService communicationOutboxService;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<PurchaseOrderDto> listOrders() {
        List<PurchaseOrderEntity> orders = purchaseOrderRepository.findAllByOrderByUpdatedAtDesc();
        Map<UUID, Product> productMap = loadProductsForOrders(orders);
        return orders.stream()
                .map(order -> toDto(order, productMap))
                .toList();
    }

    @Transactional
    public PurchaseOrderDto createOrder(CreatePurchaseOrderRequest request) {
        SupplierEntity supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new NotFoundException("Supplier not found"));
        if (!"ACTIVE".equalsIgnoreCase(supplier.getStatus())) {
            throw new ValidationException("Supplier must be active");
        }

        Map<UUID, Product> productMap = loadProductsForLineRequests(request.getLines());

        PurchaseOrderEntity order = new PurchaseOrderEntity();
        order.setSupplier(supplier);
        order.setOrderDate(LocalDate.now());
        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        order.setStatus(PurchaseOrderStatus.DRAFT);

        for (CreatePurchaseOrderLineRequest lineRequest : request.getLines()) {
            Product product = productMap.get(lineRequest.getProductId());
            if (product == null) {
                throw new NotFoundException("Product not found");
            }
            if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new ValidationException("Product must be active for purchase orders");
            }

            PurchaseOrderLineEntity line = new PurchaseOrderLineEntity();
            line.setProductId(product.getId());
            line.setQuantityOrdered(lineRequest.getQuantity());
            line.setQuantityReceived(0);
            line.setUnitPrice(resolveCostUnitPrice(lineRequest, product));
            order.addLine(line);
        }

        PurchaseOrderEntity saved = purchaseOrderRepository.saveAndFlush(order);
        entityAuditService.log("PURCHASE_ORDER", saved.getId(), "CREATE", null, saved.getStatus().name());
        eventPublisher.publishEvent(OrderChangedEvent.of("PURCHASE_ORDER_CREATED", saved.getId(), saved.getVersion()));
        return toDto(saved, productMap);
    }

    @Transactional
    public PurchaseOrderDto markOrdered(UUID id, Long expectedVersion) {
        PurchaseOrderEntity order = purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));
        ensureVersion(expectedVersion, order.getVersion(), "Purchase order version conflict");

        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new ValidationException("Only draft purchase orders can be marked ordered");
        }

        PurchaseOrderStatus oldStatus = order.getStatus();
        order.setStatus(PurchaseOrderStatus.ORDERED);
        PurchaseOrderEntity saved = purchaseOrderRepository.saveAndFlush(order);
        queueSupplierOrderNotification(saved);
        entityAuditService.log("PURCHASE_ORDER", saved.getId(), "STATUS_CHANGE", oldStatus.name(), saved.getStatus().name());
        eventPublisher.publishEvent(OrderChangedEvent.of("PURCHASE_ORDER_ORDERED", saved.getId(), saved.getVersion()));

        Map<UUID, Product> productMap = loadProductsForOrders(List.of(saved));
        return toDto(saved, productMap);
    }

    @Transactional
    public PurchaseOrderDto receiveOrder(UUID id, ReceivePurchaseOrderRequest request) {
        PurchaseOrderEntity order = purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));
        ensureVersion(request.getVersion(), order.getVersion(), "Purchase order version conflict");

        if (order.getStatus() != PurchaseOrderStatus.ORDERED && order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new ValidationException("Only ordered purchase orders can be received");
        }

        Map<UUID, PurchaseOrderLineEntity> lineMap = order.getLines()
                .stream()
                .collect(Collectors.toMap(PurchaseOrderLineEntity::getId, Function.identity()));
        Set<UUID> impactedProducts = order.getLines().stream().map(PurchaseOrderLineEntity::getProductId).collect(Collectors.toSet());
        lockProducts(impactedProducts);
        Map<UUID, Integer> requestedByLine = new HashMap<>();
        for (ReceivePurchaseOrderLineRequest lineRequest : request.getLines()) {
            PurchaseOrderLineEntity line = lineMap.get(lineRequest.getLineId());
            if (line == null) {
                throw new NotFoundException("Purchase order line not found");
            }
            requestedByLine.merge(line.getId(), lineRequest.getQuantity(), Integer::sum);
        }
        if (requestedByLine.isEmpty()) {
            throw new ValidationException("At least one receipt line is required");
        }

        for (Map.Entry<UUID, Integer> entry : requestedByLine.entrySet()) {
            PurchaseOrderLineEntity line = lineMap.get(entry.getKey());
            int quantity = entry.getValue();
            int remaining = line.getQuantityOrdered() - line.getQuantityReceived();
            if (quantity <= 0 || quantity > remaining) {
                throw new ValidationException("Receipt quantity exceeds remaining quantity");
            }
        }

        ActorService.Actor actor = actorService.getCurrentActor();
        for (Map.Entry<UUID, Integer> entry : requestedByLine.entrySet()) {
            PurchaseOrderLineEntity line = lineMap.get(entry.getKey());
            int quantity = entry.getValue();
            line.setQuantityReceived(line.getQuantityReceived() + quantity);

            InventoryTransactionEntity txn = new InventoryTransactionEntity();
            txn.setProductId(line.getProductId());
            txn.setTransactionType(InventoryTransactionType.IN);
            txn.setQuantity(quantity);
            txn.setReferenceType("PURCHASE_ORDER");
            txn.setReferenceId(order.getId());
            txn.setReferenceLineId(line.getId());
            txn.setUnitPrice(line.getUnitPrice());
            txn.setPerformedBy(actor.getUserId());
            txn.setCreatedByUsername(actor.getUsername());
            inventoryTransactionRepository.save(txn);
        }

        PurchaseOrderStatus oldStatus = order.getStatus();
        if (order.getLines().stream().allMatch(line -> Objects.equals(line.getQuantityReceived(), line.getQuantityOrdered()))) {
            order.setStatus(PurchaseOrderStatus.RECEIVED);
        } else {
            order.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        PurchaseOrderEntity saved = purchaseOrderRepository.saveAndFlush(order);
        for (UUID productId : impactedProducts) {
            salesOrderService.allocatePendingReservationsForProduct(productId);
        }
        entityAuditService.log("PURCHASE_ORDER", saved.getId(), "STATUS_CHANGE", oldStatus.name(), saved.getStatus().name());
        eventPublisher.publishEvent(OrderChangedEvent.of("PURCHASE_ORDER_RECEIVED", saved.getId(), saved.getVersion()));
        eventPublisher.publishEvent(InventoryChangedEvent.of("INVENTORY_RECEIVED", saved.getId(), saved.getVersion()));

        Map<UUID, Product> productMap = loadProductsForOrders(List.of(saved));
        return toDto(saved, productMap);
    }

    @Transactional
    public PurchaseOrderDto cancelOrder(UUID id, Long expectedVersion) {
        PurchaseOrderEntity order = purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));
        ensureVersion(expectedVersion, order.getVersion(), "Purchase order version conflict");

        if (order.getStatus() == PurchaseOrderStatus.CANCELLED || order.getStatus() == PurchaseOrderStatus.RECEIVED || order.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new ValidationException("Order cannot be cancelled in current status");
        }

        PurchaseOrderStatus oldStatus = order.getStatus();
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        PurchaseOrderEntity saved = purchaseOrderRepository.saveAndFlush(order);
        entityAuditService.log("PURCHASE_ORDER", saved.getId(), "STATUS_CHANGE", oldStatus.name(), saved.getStatus().name());
        eventPublisher.publishEvent(OrderChangedEvent.of("PURCHASE_ORDER_CANCELLED", saved.getId(), saved.getVersion()));

        Map<UUID, Product> productMap = loadProductsForOrders(List.of(saved));
        return toDto(saved, productMap);
    }

    private BigDecimal resolveCostUnitPrice(CreatePurchaseOrderLineRequest request, Product product) {
        if (request.getUnitPrice() != null) {
            return request.getUnitPrice();
        }
        if (product.getCostPrice() != null) {
            return product.getCostPrice();
        }
        return BigDecimal.ZERO;
    }

    private void queueSupplierOrderNotification(PurchaseOrderEntity order) {
        if (order.getSupplier() == null || order.getSupplier().getEmail() == null || order.getSupplier().getEmail().isBlank()) {
            return;
        }
        Map<UUID, Product> productById = productRepository.findAllById(
                        order.getLines().stream().map(PurchaseOrderLineEntity::getProductId).collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<EmailTemplateService.EmailLineItem> lineItems = order.getLines().stream()
                .map(line -> new EmailTemplateService.EmailLineItem(
                        productById.get(line.getProductId()) == null ? "Unknown Product" : productById.get(line.getProductId()).getName(),
                        productById.get(line.getProductId()) == null ? "-" : productById.get(line.getProductId()).getSku(),
                        line.getQuantityOrdered() == null ? 0 : line.getQuantityOrdered(),
                        line.getUnitPrice() == null ? BigDecimal.ZERO : line.getUnitPrice(),
                        (line.getUnitPrice() == null ? BigDecimal.ZERO : line.getUnitPrice())
                                .multiply(BigDecimal.valueOf(line.getQuantityOrdered() == null ? 0 : line.getQuantityOrdered()))))
                .toList();
        RenderedEmail email = emailTemplateService.buildPurchaseOrderIssued(
                order.getId(),
                order.getExpectedDeliveryDate(),
                lineItems
        );
        communicationOutboxService.enqueue(
                "PURCHASE_ORDER",
                order.getId(),
                order.getSupplier().getEmail(),
                email.subject(),
                email.textBody(),
                email.htmlBody()
        );
    }

    private void ensureVersion(Long expectedVersion, Long actualVersion, String message) {
        if (!Objects.equals(expectedVersion, actualVersion)) {
            throw new ConflictException(message);
        }
    }

    private Map<UUID, Product> loadProductsForOrders(List<PurchaseOrderEntity> orders) {
        Set<UUID> ids = orders.stream()
                .flatMap(order -> order.getLines().stream())
                .map(PurchaseOrderLineEntity::getProductId)
                .collect(Collectors.toSet());
        return productRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Map<UUID, Product> loadProductsForLineRequests(List<CreatePurchaseOrderLineRequest> lines) {
        Set<UUID> ids = lines.stream()
                .map(CreatePurchaseOrderLineRequest::getProductId)
                .collect(Collectors.toSet());
        return productRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private PurchaseOrderDto toDto(PurchaseOrderEntity order, Map<UUID, Product> productMap) {
        PurchaseOrderDto dto = new PurchaseOrderDto();
        dto.setId(order.getId());
        dto.setSupplierId(order.getSupplier() == null ? null : order.getSupplier().getId());
        dto.setSupplierName(order.getSupplier() == null ? null : order.getSupplier().getName());
        dto.setDate(order.getOrderDate());
        dto.setExpectedDeliveryDate(order.getExpectedDeliveryDate());
        dto.setStatus(order.getStatus().name());
        dto.setVersion(order.getVersion());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        List<PurchaseOrderLineDto> lineDtos = new ArrayList<>();
        for (PurchaseOrderLineEntity line : order.getLines()) {
            Product product = productMap.get(line.getProductId());
            PurchaseOrderLineDto lineDto = new PurchaseOrderLineDto();
            lineDto.setId(line.getId());
            lineDto.setProductId(line.getProductId());
            lineDto.setProductName(product == null ? null : product.getName());
            lineDto.setQuantityOrdered(line.getQuantityOrdered());
            lineDto.setQuantityReceived(line.getQuantityReceived());
            lineDto.setUnitPrice(line.getUnitPrice());
            lineDto.setLineTotal(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantityOrdered())));
            lineDtos.add(lineDto);
        }
        dto.setLines(lineDtos);
        return dto;
    }

    private void lockProducts(Set<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }
        List<Product> locked = productRepository.findAllByIdInForUpdate(productIds);
        if (locked.size() != productIds.size()) {
            throw new NotFoundException("Product not found");
        }
    }
}
