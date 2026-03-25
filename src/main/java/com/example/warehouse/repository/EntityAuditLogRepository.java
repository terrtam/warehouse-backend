package com.example.warehouse.repository;

import com.example.warehouse.entity.EntityAuditLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EntityAuditLogRepository extends JpaRepository<EntityAuditLogEntity, UUID> {

    List<EntityAuditLogEntity> findTop200ByOrderByCreatedAtDesc();

    @Query("""
            select e from EntityAuditLogEntity e
            where (:entityType is null or upper(e.entityType) = upper(:entityType))
              and (:action is null or upper(e.action) = upper(:action))
              and (:entityId is null or e.entityId = :entityId)
            order by e.createdAt desc
            """)
    List<EntityAuditLogEntity> findFiltered(
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("entityId") UUID entityId,
            Pageable pageable
    );
}
