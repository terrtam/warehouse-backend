package com.example.warehouse.repository;

import com.example.warehouse.entity.CommunicationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommunicationLogRepository extends
        JpaRepository<CommunicationLogEntity, UUID>,
        JpaSpecificationExecutor<CommunicationLogEntity> {

    List<CommunicationLogEntity> findTop200ByOrderByCreatedAtDesc();
}
