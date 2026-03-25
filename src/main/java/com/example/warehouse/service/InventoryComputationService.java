package com.example.warehouse.service;

import com.example.warehouse.entity.InventoryTransactionEntity;
import com.example.warehouse.entity.InventoryTransactionType;
import com.example.warehouse.entity.SalesOrderStatus;
import com.example.warehouse.repository.InventoryTransactionRepository;
import com.example.warehouse.repository.SalesOrderLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryComputationService {

    private static final List<SalesOrderStatus> RESERVATION_ACTIVE_STATUSES = List.of(
            SalesOrderStatus.PROCESSING,
            SalesOrderStatus.PARTIALLY_SHIPPED
    );

    @Autowired
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Autowired
    private SalesOrderLineRepository salesOrderLineRepository;

    public int onHandForProduct(UUID productId) {
        return inventoryTransactionRepository.findAllByProductId(productId)
                .stream()
                .mapToInt(this::toSignedQuantity)
                .sum();
    }

    public int reservedForProduct(UUID productId) {
        Integer reserved = salesOrderLineRepository.sumReservedByProductAndStatuses(
                productId,
                RESERVATION_ACTIVE_STATUSES
        );
        return reserved == null ? 0 : reserved;
    }

    public int availableForProduct(UUID productId) {
        return onHandForProduct(productId) - reservedForProduct(productId);
    }

    private int toSignedQuantity(InventoryTransactionEntity transaction) {
        if (transaction.getTransactionType() == InventoryTransactionType.OUT) {
            return -transaction.getQuantity();
        }
        return transaction.getQuantity();
    }
}
