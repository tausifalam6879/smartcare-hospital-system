package com.smartcare.auth.repository;

import com.smartcare.auth.domain.StaffAccessRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface StaffAccessRequestRepository extends JpaRepository<StaffAccessRequest, UUID> {
    boolean existsByHospitalIdAndMobileNumberAndAccountTypeAndStatus(UUID hospitalId, String mobileNumber,
            String accountType, String status);
    Page<StaffAccessRequest> findAllByHospitalIdAndStatus(UUID hospitalId, String status, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from StaffAccessRequest r where r.id = :id")
    Optional<StaffAccessRequest> findLockedById(@Param("id") UUID id);
}
