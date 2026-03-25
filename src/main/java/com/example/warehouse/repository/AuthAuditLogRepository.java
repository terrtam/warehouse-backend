package com.example.warehouse.repository;

import com.example.warehouse.entity.AuthAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLogEntity, UUID> {
}
