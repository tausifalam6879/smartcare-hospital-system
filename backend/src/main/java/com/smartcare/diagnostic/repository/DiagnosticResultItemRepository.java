package com.smartcare.diagnostic.repository;

import com.smartcare.diagnostic.domain.DiagnosticResultItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiagnosticResultItemRepository extends JpaRepository<DiagnosticResultItem, UUID> {
    List<DiagnosticResultItem> findAllByResultIdOrderByItemOrderAsc(UUID resultId);
}
