package com.example.warehouse.repository;

import com.example.warehouse.entity.SalesOrderEntity;
import com.example.warehouse.entity.SalesOrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrderEntity, UUID> {

    @EntityGraph(attributePaths = {"customer", "lines", "lines.supplier"})
    List<SalesOrderEntity> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"customer", "lines", "lines.supplier"})
    @Query("select o from SalesOrderEntity o where o.id = :id")
    java.util.Optional<SalesOrderEntity> findByIdWithDetails(@Param("id") UUID id);

    @Query("select coalesce(sum(l.quantityReserved), 0) from SalesOrderLineEntity l " +
            "join l.salesOrder o where l.productId = :productId and o.status in :statuses")
    Integer sumReservedQuantityByProduct(@Param("productId") UUID productId, @Param("statuses") List<SalesOrderStatus> statuses);
}
