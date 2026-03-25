package com.example.warehouse.repository;

import com.example.warehouse.entity.PurchaseOrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrderEntity, UUID> {

    @EntityGraph(attributePaths = {"supplier", "lines"})
    List<PurchaseOrderEntity> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"supplier", "lines"})
    @Query("select o from PurchaseOrderEntity o where o.id = :id")
    java.util.Optional<PurchaseOrderEntity> findByIdWithDetails(@Param("id") UUID id);
}
