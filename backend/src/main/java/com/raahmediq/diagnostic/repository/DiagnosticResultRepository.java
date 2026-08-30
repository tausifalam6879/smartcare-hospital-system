package com.raahmediq.diagnostic.repository;

import com.raahmediq.diagnostic.domain.DiagnosticResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DiagnosticResultRepository extends JpaRepository<DiagnosticResult, UUID> {
    Optional<DiagnosticResult> findByOrderId(UUID orderId);
}
