package com.example.warehouse.repository;

import com.example.warehouse.entity.SalesOrderLineEntity;
import com.example.warehouse.entity.SalesOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLineEntity, UUID> {

    @Query("select l from SalesOrderLineEntity l join fetch l.salesOrder o " +
            "where l.productId = :productId and o.status in :statuses " +
            "and (l.quantityOrdered - l.quantityShipped - l.quantityReserved) > 0 " +
            "order by o.createdAt asc, l.createdAt asc")
    List<SalesOrderLineEntity> findBackorderedLinesForAllocation(
            @Param("productId") UUID productId,
            @Param("statuses") List<SalesOrderStatus> statuses
    );

    @Query("select coalesce(sum(l.quantityReserved), 0) from SalesOrderLineEntity l " +
            "join l.salesOrder o where l.productId = :productId and o.status in :statuses")
    Integer sumReservedByProductAndStatuses(@Param("productId") UUID productId, @Param("statuses") List<SalesOrderStatus> statuses);
}
