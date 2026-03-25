package com.example.warehouse.service;

import com.example.warehouse.dto.CreateSalesOrderLineRequest;
import com.example.warehouse.dto.CreateSalesOrderRequest;
import com.example.warehouse.dto.SalesOrderDto;
import com.example.warehouse.dto.SalesOrderLineDto;
import com.example.warehouse.dto.ShipSalesOrderLineRequest;
import com.example.warehouse.dto.ShipSalesOrderRequest;
import com.example.warehouse.entity.CustomerEntity;
import com.example.warehouse.entity.InventoryTransactionEntity;
import com.example.warehouse.entity.InventoryTransactionType;
import com.example.warehouse.entity.Product;
import com.example.warehouse.entity.SalesOrderEntity;
import com.example.warehouse.entity.SalesOrderLineEntity;
import com.example.warehouse.entity.SalesOrderStatus;
import com.example.warehouse.entity.SupplierEntity;
import com.example.warehouse.event.InventoryChangedEvent;
import com.example.warehouse.event.OrderChangedEvent;
import com.example.warehouse.exception.ConflictException;
import com.example.warehouse.exception.NotFoundException;
import com.example.warehouse.exception.ValidationException;
import com.example.warehouse.repository.CustomerRepository;
import com.example.warehouse.repository.InventoryTransactionRepository;
import com.example.warehouse.repository.ProductRepository;
import com.example.warehouse.repository.SalesOrderLineRepository;
import com.example.warehouse.repository.SalesOrderRepository;
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
public class SalesOrderService {

    private static final List<SalesOrderStatus> RESERVATION_ACTIVE_STATUSES = List.of(
            SalesOrderStatus.PROCESSING,
            SalesOrderStatus.PARTIALLY_SHIPPED
    );

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderLineRepository salesOrderLineRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private InventoryComputationService inventoryComputationService;

    @Autowired
    private ActorService actorService;

    @Autowired
    private EntityAuditService entityAuditService;

    @Autowired
    private CommunicationOutboxService communicationOutboxService;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<SalesOrderDto> listOrders() {
        List<SalesOrderEntity> orders = salesOrderRepository.findAllByOrderByUpdatedAtDesc();
        Map<UUID, Product> productMap = loadProductsForOrders(orders);
        return orders.stream()
                .map(order -> toDto(order, productMap))
                .toList();
    }

    @Transactional
    public SalesOrderDto createOrder(CreateSalesOrderRequest request) {
        CustomerEntity customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        if (!"ACTIVE".equalsIgnoreCase(customer.getStatus())) {
            throw new ValidationException("Customer must be active");
        }

        Map<UUID, Product> productMap = loadProductsForLineRequests(request.getLines());
        Map<UUID, SupplierEntity> supplierMap = loadSuppliersForLineRequests(request.getLines());
        UUID firstLineSupplierId = request.getLines().get(0).getSupplierId();
        for (CreateSalesOrderLineRequest lineRequest : request.getLines()) {
            if (!Objects.equals(firstLineSupplierId, lineRequest.getSupplierId())) {
                throw new ValidationException("All sales order lines must use the same supplier as the first line");
            }
        }

        SupplierEntity supplier = firstLineSupplierId == null ? null : supplierMap.get(firstLineSupplierId);
        if (firstLineSupplierId != null) {
            if (supplier == null) {
                throw new NotFoundException("Supplier not found");
            }
            if (!"ACTIVE".equalsIgnoreCase(supplier.getStatus())) {
                throw new ValidationException("Supplier must be active for sales orders");
            }
        }

        SalesOrderEntity order = new SalesOrderEntity();
        order.setCustomer(customer);
        order.setOrderDate(LocalDate.now());
        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        order.setStatus(SalesOrderStatus.DRAFT);

        for (CreateSalesOrderLineRequest lineRequest : request.getLines()) {
            Product product = productMap.get(lineRequest.getProductId());
            if (product == null) {
                throw new NotFoundException("Product not found");
            }
            if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new ValidationException("Product must be active for sales orders");
            }

            SalesOrderLineEntity line = new SalesOrderLineEntity();
            line.setProductId(product.getId());
            line.setSupplier(supplier);
            line.setQuantityOrdered(lineRequest.getQuantity());
            line.setQuantityReserved(0);
            line.setQuantityShipped(0);
            line.setUnitPrice(resolveSaleUnitPrice(lineRequest, product));
            order.addLine(line);
        }

        SalesOrderEntity saved = salesOrderRepository.saveAndFlush(order);
        entityAuditService.log("SALES_ORDER", saved.getId(), "CREATE", null, saved.getStatus().name());
        eventPublisher.publishEvent(OrderChangedEvent.of("SALES_ORDER_CREATED", saved.getId(), saved.getVersion()));

        return toDto(saved, productMap);
    }

    @Transactional
    public SalesOrderDto confirmOrder(UUID id, Long expectedVersion) {
        SalesOrderEntity order = salesOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Sales order not found"));
        ensureVersion(expectedVersion, order.getVersion(), "Sales order version conflict");

        if (order.getStatus() != SalesOrderStatus.DRAFT) {
            throw new ValidationException("Only draft sales orders can be confirmed");
        }

        Map<UUID, Integer> availableByProduct = new HashMap<>();
        Set<UUID> productIds = order.getLines().stream().map(SalesOrderLineEntity::getProductId).collect(Collectors.toSet());
        lockProducts(productIds);
        for (UUID productId : productIds) {
            availableByProduct.put(productId, Math.max(0, inventoryComputationService.availableForProduct(productId)));
        }

        for (SalesOrderLineEntity line : order.getLines()) {
            int outstanding = line.getQuantityOrdered() - line.getQuantityShipped() - line.getQuantityReserved();
            if (outstanding <= 0) {
                continue;
            }
            int available = availableByProduct.getOrDefault(line.getProductId(), 0);
            int reserved = Math.min(outstanding, available);
            if (reserved > 0) {
                line.setQuantityReserved(line.getQuantityReserved() + reserved);
                availableByProduct.put(line.getProductId(), available - reserved);
            }
        }

        SalesOrderStatus oldStatus = order.getStatus();
        order.setStatus(SalesOrderStatus.PROCESSING);
        SalesOrderEntity saved = salesOrderRepository.saveAndFlush(order);

        queueOrderConfirmation(saved);
        entityAuditService.log("SALES_ORDER", saved.getId(), "STATUS_CHANGE", oldStatus.name(), saved.getStatus().name());
        eventPublisher.publishEvent(OrderChangedEvent.of("SALES_ORDER_CONFIRMED", saved.getId(), saved.getVersion()));
        eventPublisher.publishEvent(InventoryChangedEvent.of("INVENTORY_RESERVED", saved.getId(), saved.getVersion()));

        Map<UUID, Product> productMap = loadProductsForOrders(List.of(saved));
        return toDto(saved, productMap);
    }

    @Transactional
    public SalesOrderDto shipOrder(UUID id, ShipSalesOrderRequest request) {
        SalesOrderEntity order = salesOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Sales order not found"));
        ensureVersion(request.getVersion(), order.getVersion(), "Sales order version conflict");

        if (order.getStatus() != SalesOrderStatus.PROCESSING && order.getStatus() != SalesOrderStatus.PARTIALLY_SHIPPED) {
            throw new ValidationException("Only processing sales orders can be shipped");
        }

        Map<UUID, SalesOrderLineEntity> lineMap = order.getLines()
                .stream()
                .collect(Collectors.toMap(SalesOrderLineEntity::getId, Function.identity()));

        Map<UUID, Integer> requestedByLine = new HashMap<>();
        for (ShipSalesOrderLineRequest lineRequest : request.getLines()) {
            SalesOrderLineEntity line = lineMap.get(lineRequest.getLineId());
            if (line == null) {
                throw new NotFoundException("Sales order line not found");
            }
            requestedByLine.merge(line.getId(), lineRequest.getQuantity(), Integer::sum);
        }

        if (requestedByLine.isEmpty()) {
            throw new ValidationException("At least one shipment line is required");
        }

        Map<UUID, Integer> onHandByProduct = new HashMap<>();
        lockProducts(order.getLines().stream().map(SalesOrderLineEntity::getProductId).collect(Collectors.toSet()));
        for (SalesOrderLineEntity line : order.getLines()) {
            onHandByProduct.computeIfAbsent(line.getProductId(), inventoryComputationService::onHandForProduct);
        }

        for (Map.Entry<UUID, Integer> entry : requestedByLine.entrySet()) {
            SalesOrderLineEntity line = lineMap.get(entry.getKey());
            int quantity = entry.getValue();
            int remaining = line.getQuantityOrdered() - line.getQuantityShipped();
            if (quantity <= 0 || quantity > remaining) {
                throw new ValidationException("Shipment quantity exceeds remaining quantity");
            }
            if (quantity > line.getQuantityReserved()) {
                throw new ValidationException("Shipment quantity exceeds reserved quantity");
            }
            int onHand = onHandByProduct.getOrDefault(line.getProductId(), 0);
            if (quantity > onHand) {
                throw new ValidationException("Insufficient on-hand inventory for shipment");
            }
            onHandByProduct.put(line.getProductId(), onHand - quantity);
        }

        ActorService.Actor actor = actorService.getCurrentActor();
        for (Map.Entry<UUID, Integer> entry : requestedByLine.entrySet()) {
            SalesOrderLineEntity line = lineMap.get(entry.getKey());
            int quantity = entry.getValue();

            line.setQuantityReserved(line.getQuantityReserved() - quantity);
            line.setQuantityShipped(line.getQuantityShipped() + quantity);

            InventoryTransactionEntity txn = new InventoryTransactionEntity();
            txn.setProductId(line.getProductId());
            txn.setTransactionType(InventoryTransactionType.OUT);
            txn.setQuantity(quantity);
            txn.setReferenceType("SALES_ORDER");
            txn.setReferenceId(order.getId());
            txn.setReferenceLineId(line.getId());
            txn.setUnitPrice(line.getUnitPrice());
            txn.setPerformedBy(actor.getUserId());
            txn.setCreatedByUsername(actor.getUsername());
            inventoryTransactionRepository.save(txn);
        }

        SalesOrderStatus oldStatus = order.getStatus();
        if (order.getLines().stream().allMatch(line -> Objects.equals(line.getQuantityShipped(), line.getQuantityOrdered()))) {
            order.setStatus(SalesOrderStatus.SHIPPED);
        } else {
            order.setStatus(SalesOrderStatus.PARTIALLY_SHIPPED);
        }

        SalesOrderEntity saved = salesOrderRepository.saveAndFlush(order);
        queueShipmentNotification(saved);
        entityAuditService.log("SALES_ORDER", saved.getId(), "STATUS_CHANGE", oldStatus.name(), saved.getStatus().name());
        eventPublisher.publishEvent(OrderChangedEvent.of("SALES_ORDER_SHIPPED", saved.getId(), saved.getVersion()));
        eventPublisher.publishEvent(InventoryChangedEvent.of("INVENTORY_SHIPPED", saved.getId(), saved.getVersion()));

        Map<UUID, Product> productMap = loadProductsForOrders(List.of(saved));
        return toDto(saved, productMap);
    }

    @Transactional
    public SalesOrderDto cancelOrder(UUID id, Long expectedVersion) {
        SalesOrderEntity order = salesOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new NotFoundException("Sales order not found"));
        ensureVersion(expectedVersion, order.getVersion(), "Sales order version conflict");

        if (order.getStatus() == SalesOrderStatus.CANCELLED || order.getStatus() == SalesOrderStatus.SHIPPED) {
            throw new ValidationException("Order cannot be cancelled in current status");
        }
        boolean hasShippedLine = order.getLines()
                .stream()
                .anyMatch(line -> line.getQuantityShipped() != null && line.getQuantityShipped() > 0);
        if (hasShippedLine) {
            throw new ValidationException("Partially shipped orders cannot be cancelled");
        }

        SalesOrderStatus oldStatus = order.getStatus();
        for (SalesOrderLineEntity line : order.getLines()) {
            line.setQuantityReserved(0);
        }
        order.setStatus(SalesOrderStatus.CANCELLED);
        SalesOrderEntity saved = salesOrderRepository.saveAndFlush(order);
        entityAuditService.log("SALES_ORDER", saved.getId(), "STATUS_CHANGE", oldStatus.name(), saved.getStatus().name());
        eventPublisher.publishEvent(OrderChangedEvent.of("SALES_ORDER_CANCELLED", saved.getId(), saved.getVersion()));
        eventPublisher.publishEvent(InventoryChangedEvent.of("INVENTORY_RELEASED", saved.getId(), saved.getVersion()));

        Map<UUID, Product> productMap = loadProductsForOrders(List.of(saved));
        return toDto(saved, productMap);
    }

    @Transactional
    public void allocatePendingReservationsForProduct(UUID productId) {
        lockProducts(Set.of(productId));
        int available = Math.max(0, inventoryComputationService.availableForProduct(productId));
        if (available <= 0) {
            return;
        }

        List<SalesOrderLineEntity> lines = salesOrderLineRepository.findBackorderedLinesForAllocation(
                productId,
                RESERVATION_ACTIVE_STATUSES
        );

        boolean changed = false;
        for (SalesOrderLineEntity line : lines) {
            int outstanding = line.getQuantityOrdered() - line.getQuantityShipped() - line.getQuantityReserved();
            if (outstanding <= 0) {
                continue;
            }
            int reserve = Math.min(outstanding, available);
            if (reserve <= 0) {
                break;
            }
            line.setQuantityReserved(line.getQuantityReserved() + reserve);
            available -= reserve;
            changed = true;
        }

        if (changed) {
            salesOrderLineRepository.saveAll(lines);
            eventPublisher.publishEvent(InventoryChangedEvent.of("INVENTORY_REALLOCATED", productId, null));
        }
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

    private BigDecimal resolveSaleUnitPrice(CreateSalesOrderLineRequest request, Product product) {
        if (request.getUnitPrice() != null) {
            return request.getUnitPrice();
        }
        if (product.getDefaultSalePrice() != null) {
            return product.getDefaultSalePrice();
        }
        return BigDecimal.ZERO;
    }

    private void queueOrderConfirmation(SalesOrderEntity order) {
        if (order.getCustomer() == null || order.getCustomer().getEmail() == null || order.getCustomer().getEmail().isBlank()) {
            return;
        }
        Map<UUID, Product> productById = productRepository.findAllById(
                        order.getLines().stream().map(SalesOrderLineEntity::getProductId).collect(Collectors.toSet())
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
        RenderedEmail email = emailTemplateService.buildSalesOrderConfirmed(
                order.getId(),
                order.getCustomer().getName(),
                order.getCustomer().getAddress(),
                order.getExpectedDeliveryDate(),
                lineItems
        );
        communicationOutboxService.enqueue(
                "SALES_ORDER",
                order.getId(),
                order.getCustomer().getEmail(),
                email.subject(),
                email.textBody(),
                email.htmlBody()
        );
    }

    private void queueShipmentNotification(SalesOrderEntity order) {
        if (order.getCustomer() == null || order.getCustomer().getEmail() == null || order.getCustomer().getEmail().isBlank()) {
            return;
        }
        Map<UUID, Product> productById = productRepository.findAllById(
                        order.getLines().stream().map(SalesOrderLineEntity::getProductId).collect(Collectors.toSet())
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
        RenderedEmail email = emailTemplateService.buildSalesOrderShipped(
                order.getId(),
                order.getCustomer().getName(),
                order.getCustomer().getAddress(),
                order.getExpectedDeliveryDate(),
                lineItems
        );
        communicationOutboxService.enqueue(
                "SALES_ORDER",
                order.getId(),
                order.getCustomer().getEmail(),
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

    private Map<UUID, Product> loadProductsForOrders(List<SalesOrderEntity> orders) {
        Set<UUID> ids = orders.stream()
                .flatMap(order -> order.getLines().stream())
                .map(SalesOrderLineEntity::getProductId)
                .collect(Collectors.toSet());
        return productRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Map<UUID, Product> loadProductsForLineRequests(List<CreateSalesOrderLineRequest> lines) {
        Set<UUID> ids = lines.stream()
                .map(CreateSalesOrderLineRequest::getProductId)
                .collect(Collectors.toSet());
        return productRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private Map<UUID, SupplierEntity> loadSuppliersForLineRequests(List<CreateSalesOrderLineRequest> lines) {
        Set<UUID> ids = lines.stream()
                .map(CreateSalesOrderLineRequest::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return supplierRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(SupplierEntity::getId, Function.identity()));
    }

    private SalesOrderDto toDto(SalesOrderEntity order, Map<UUID, Product> productMap) {
        SalesOrderDto dto = new SalesOrderDto();
        dto.setId(order.getId());
        dto.setCustomerId(order.getCustomer() == null ? null : order.getCustomer().getId());
        dto.setCustomerName(order.getCustomer() == null ? null : order.getCustomer().getName());
        dto.setDate(order.getOrderDate());
        dto.setExpectedDeliveryDate(order.getExpectedDeliveryDate());
        dto.setStatus(order.getStatus().name());
        dto.setVersion(order.getVersion());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        List<SalesOrderLineDto> lineDtos = new ArrayList<>();
        for (SalesOrderLineEntity line : order.getLines()) {
            Product product = productMap.get(line.getProductId());
            SalesOrderLineDto lineDto = new SalesOrderLineDto();
            lineDto.setId(line.getId());
            lineDto.setProductId(line.getProductId());
            lineDto.setProductName(product == null ? null : product.getName());
            lineDto.setSupplierId(line.getSupplier() == null ? null : line.getSupplier().getId());
            lineDto.setSupplierName(line.getSupplier() == null ? null : line.getSupplier().getName());
            lineDto.setQuantityOrdered(line.getQuantityOrdered());
            lineDto.setQuantityReserved(line.getQuantityReserved());
            lineDto.setQuantityShipped(line.getQuantityShipped());
            lineDto.setUnitPrice(line.getUnitPrice());
            lineDto.setLineTotal(line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantityOrdered())));
            lineDtos.add(lineDto);
        }
        dto.setLines(lineDtos);
        return dto;
    }
}
