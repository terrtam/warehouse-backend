package com.example.warehouse.service;

import com.example.warehouse.dto.InventoryRecordDto;
import com.example.warehouse.entity.InventoryTransactionEntity;
import com.example.warehouse.entity.InventoryTransactionType;
import com.example.warehouse.entity.Product;
import com.example.warehouse.entity.PurchaseOrderEntity;
import com.example.warehouse.entity.PurchaseOrderLineEntity;
import com.example.warehouse.entity.PurchaseOrderStatus;
import com.example.warehouse.entity.SalesOrderEntity;
import com.example.warehouse.entity.SalesOrderLineEntity;
import com.example.warehouse.entity.SalesOrderStatus;
import com.example.warehouse.repository.InventoryTransactionRepository;
import com.example.warehouse.repository.ProductRepository;
import com.example.warehouse.repository.PurchaseOrderRepository;
import com.example.warehouse.repository.SalesOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> salesByProduct(LocalDate from, LocalDate to) {
        List<SalesOrderEntity> orders = salesOrderRepository.findAllByOrderByUpdatedAtDesc();
        Map<UUID, Product> products = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Map<UUID, BigDecimal> revenueByProduct = new HashMap<>();
        Map<UUID, Integer> qtyByProduct = new HashMap<>();
        for (SalesOrderEntity order : orders) {
            if (order.getStatus() != SalesOrderStatus.SHIPPED && order.getStatus() != SalesOrderStatus.PARTIALLY_SHIPPED) {
                continue;
            }
            if (isOutsideRange(order.getOrderDate(), from, to)) {
                continue;
            }
            for (SalesOrderLineEntity line : order.getLines()) {
                if (line.getQuantityShipped() <= 0) {
                    continue;
                }
                BigDecimal revenue = line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantityShipped()));
                revenueByProduct.merge(line.getProductId(), revenue, BigDecimal::add);
                qtyByProduct.merge(line.getProductId(), line.getQuantityShipped(), Integer::sum);
            }
        }

        return revenueByProduct.entrySet().stream()
                .map(entry -> {
                    Product product = products.get(entry.getKey());
                    Map<String, Object> row = new HashMap<>();
                    row.put("productId", entry.getKey());
                    row.put("productName", product == null ? null : product.getName());
                    row.put("sku", product == null ? null : product.getSku());
                    row.put("shippedQuantity", qtyByProduct.getOrDefault(entry.getKey(), 0));
                    row.put("revenue", entry.getValue());
                    return row;
                })
                .sorted(Comparator.comparing(row -> String.valueOf(row.get("productName")), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> salesByCategory(LocalDate from, LocalDate to) {
        List<Map<String, Object>> byProduct = salesByProduct(from, to);
        Map<String, BigDecimal> revenueByCategory = new HashMap<>();
        Map<String, Integer> qtyByCategory = new HashMap<>();

        Map<String, Product> productById = productRepository.findAll().stream()
                .collect(Collectors.toMap(product -> product.getId().toString(), Function.identity()));

        for (Map<String, Object> row : byProduct) {
            String productId = String.valueOf(row.get("productId"));
            Product product = productById.get(productId);
            String category = product == null || product.getCategoryName() == null
                    ? "Uncategorized"
                    : product.getCategoryName();
            BigDecimal revenue = (BigDecimal) row.get("revenue");
            Integer quantity = (Integer) row.get("shippedQuantity");
            revenueByCategory.merge(category, revenue, BigDecimal::add);
            qtyByCategory.merge(category, quantity, Integer::sum);
        }

        return revenueByCategory.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("categoryName", entry.getKey());
                    row.put("shippedQuantity", qtyByCategory.getOrDefault(entry.getKey(), 0));
                    row.put("revenue", entry.getValue());
                    return row;
                })
                .sorted(Comparator.comparing(row -> String.valueOf(row.get("categoryName")), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> purchaseCostTracking(LocalDate from, LocalDate to) {
        List<PurchaseOrderEntity> orders = purchaseOrderRepository.findAllByOrderByUpdatedAtDesc();
        List<Map<String, Object>> rows = new ArrayList<>();

        for (PurchaseOrderEntity order : orders) {
            if (order.getStatus() != PurchaseOrderStatus.RECEIVED && order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
                continue;
            }
            if (isOutsideRange(order.getOrderDate(), from, to)) {
                continue;
            }

            BigDecimal total = order.getLines().stream()
                    .map(line -> line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantityReceived())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> row = new HashMap<>();
            row.put("purchaseOrderId", order.getId());
            row.put("supplierId", order.getSupplier() == null ? null : order.getSupplier().getId());
            row.put("supplierName", order.getSupplier() == null ? null : order.getSupplier().getName());
            row.put("receivedCost", total);
            row.put("orderDate", order.getOrderDate());
            rows.add(row);
        }

        rows.sort(Comparator.comparing(row -> String.valueOf(row.get("orderDate"))));
        return rows;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> supplierPerformance(LocalDate from, LocalDate to) {
        List<PurchaseOrderEntity> orders = purchaseOrderRepository.findAllByOrderByUpdatedAtDesc();
        Map<UUID, Integer> totalBySupplier = new HashMap<>();
        Map<UUID, Integer> receivedBySupplier = new HashMap<>();
        Map<UUID, Long> leadDaysBySupplier = new HashMap<>();
        Map<UUID, Integer> leadSamplesBySupplier = new HashMap<>();
        Map<UUID, String> names = new HashMap<>();

        for (PurchaseOrderEntity order : orders) {
            if (isOutsideRange(order.getOrderDate(), from, to)) {
                continue;
            }
            if (order.getSupplier() == null) {
                continue;
            }

            UUID supplierId = order.getSupplier().getId();
            names.put(supplierId, order.getSupplier().getName());
            totalBySupplier.merge(supplierId, 1, Integer::sum);

            if (order.getStatus() == PurchaseOrderStatus.RECEIVED || order.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED) {
                receivedBySupplier.merge(supplierId, 1, Integer::sum);
                if (order.getUpdatedAt() != null && order.getOrderDate() != null) {
                    long days = java.time.Duration.between(order.getOrderDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC), order.getUpdatedAt()).toDays();
                    leadDaysBySupplier.merge(supplierId, days, Long::sum);
                    leadSamplesBySupplier.merge(supplierId, 1, Integer::sum);
                }
            }
        }

        return names.entrySet().stream()
                .map(entry -> {
                    UUID supplierId = entry.getKey();
                    int total = totalBySupplier.getOrDefault(supplierId, 0);
                    int received = receivedBySupplier.getOrDefault(supplierId, 0);
                    int samples = leadSamplesBySupplier.getOrDefault(supplierId, 0);
                    double avgLeadDays = samples == 0 ? 0 : (double) leadDaysBySupplier.getOrDefault(supplierId, 0L) / samples;

                    Map<String, Object> row = new HashMap<>();
                    row.put("supplierId", supplierId);
                    row.put("supplierName", entry.getValue());
                    row.put("totalOrders", total);
                    row.put("receivedOrders", received);
                    row.put("receiveRate", total == 0 ? 0.0 : (double) received / total);
                    row.put("avgLeadDays", avgLeadDays);
                    return row;
                })
                .sorted(Comparator.comparing(row -> String.valueOf(row.get("supplierName")), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> velocity(LocalDate from, LocalDate to) {
        List<InventoryTransactionEntity> txns = inventoryTransactionRepository.findAllByOrderByCreatedAtDesc();
        Map<UUID, Integer> inbound = new HashMap<>();
        Map<UUID, Integer> outbound = new HashMap<>();
        Map<UUID, Product> products = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (InventoryTransactionEntity txn : txns) {
            LocalDate createdDate = txn.getCreatedAt() == null ? null : txn.getCreatedAt().atOffset(java.time.ZoneOffset.UTC).toLocalDate();
            if (isOutsideRange(createdDate, from, to)) {
                continue;
            }
            if (txn.getTransactionType() == InventoryTransactionType.IN) {
                inbound.merge(txn.getProductId(), txn.getQuantity(), Integer::sum);
            } else if (txn.getTransactionType() == InventoryTransactionType.OUT) {
                outbound.merge(txn.getProductId(), txn.getQuantity(), Integer::sum);
            }
        }

        Set<UUID> productIds = products.keySet();
        return productIds.stream()
                .map(productId -> {
                    Product product = products.get(productId);
                    int inQty = inbound.getOrDefault(productId, 0);
                    int outQty = outbound.getOrDefault(productId, 0);
                    Map<String, Object> row = new HashMap<>();
                    row.put("productId", productId);
                    row.put("productName", product == null ? null : product.getName());
                    row.put("sku", product == null ? null : product.getSku());
                    row.put("inbound", inQty);
                    row.put("outbound", outQty);
                    row.put("net", inQty - outQty);
                    return row;
                })
                .sorted(Comparator.comparing(row -> String.valueOf(row.get("productName")), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> lowStockTrends() {
        List<InventoryRecordDto> records = inventoryService.listInventory();
        Map<UUID, InstantHolder> lastMovementByProduct = new HashMap<>();

        for (InventoryTransactionEntity txn : inventoryTransactionRepository.findAllByOrderByCreatedAtDesc()) {
            lastMovementByProduct.putIfAbsent(txn.getProductId(), new InstantHolder(txn.getCreatedAt()));
        }

        return records.stream()
                .map(record -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("productId", record.getProductId());
                    row.put("productName", record.getProductName());
                    row.put("sku", record.getSku());
                    row.put("onHand", record.getOnHand());
                    row.put("reserved", record.getReserved());
                    row.put("available", record.getAvailable());
                    row.put("reorderThreshold", record.getReorderThreshold());
                    row.put("lowStock", record.isLowStock());
                    row.put("lastMovementAt", lastMovementByProduct.get(record.getProductId()) == null
                            ? null
                            : lastMovementByProduct.get(record.getProductId()).value);
                    return row;
                })
                .sorted(Comparator.comparing(row -> String.valueOf(row.get("productName")), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean isOutsideRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null) {
            return false;
        }
        if (from != null && date.isBefore(from)) {
            return true;
        }
        return to != null && date.isAfter(to);
    }

    private static class InstantHolder {
        private final java.time.Instant value;

        private InstantHolder(java.time.Instant value) {
            this.value = value;
        }
    }
}
