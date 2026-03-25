package com.example.warehouse.repository;

import com.example.warehouse.entity.PurchaseOrderLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLineEntity, UUID> {
}
