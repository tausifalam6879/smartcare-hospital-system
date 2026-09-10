package com.smartcare.ambulance.repository;

import com.smartcare.ambulance.domain.AmbulanceRequestEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AmbulanceRequestEventRepository extends JpaRepository<AmbulanceRequestEvent, UUID> {
    List<AmbulanceRequestEvent> findAllByRequestIdOrderByEventAtAsc(UUID requestId);
}
