package com.example.warehouse.repository;

import com.example.warehouse.entity.InventoryTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransactionEntity, UUID> {

    List<InventoryTransactionEntity> findAllByOrderByCreatedAtDesc();

    List<InventoryTransactionEntity> findAllByProductId(UUID productId);
}
