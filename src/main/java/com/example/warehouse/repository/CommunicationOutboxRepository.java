package com.example.warehouse.repository;

import com.example.warehouse.entity.CommunicationOutboxEntity;
import com.example.warehouse.entity.CommunicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface CommunicationOutboxRepository extends JpaRepository<CommunicationOutboxEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<CommunicationOutboxEntity> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            CommunicationStatus status,
            Instant nextAttemptAt
    );
}
