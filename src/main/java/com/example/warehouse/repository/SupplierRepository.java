package com.example.warehouse.repository;

import com.example.warehouse.entity.SupplierEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<SupplierEntity, UUID> {

    Page<SupplierEntity> findAllByUpdatedAtGreaterThan(Instant updatedAfter, Pageable pageable);
}
