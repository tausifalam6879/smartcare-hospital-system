package com.smartcare.auth.repository;

import com.smartcare.auth.domain.StaffInvitation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, UUID> {
    Page<StaffInvitation> findAllByHospitalId(UUID hospitalId, Pageable pageable);
    @Query("select count(i) > 0 from StaffInvitation i where i.hospitalId = :hospitalId "
            + "and (i.mobileNumber = :mobile or i.doctorId = :doctorId) "
            + "and i.consumedAt is null and i.revokedAt is null and i.expiresAt > :now")
    boolean hasActiveInvitation(@Param("hospitalId") UUID hospitalId, @Param("mobile") String mobile,
            @Param("doctorId") UUID doctorId, @Param("now") Instant now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from StaffInvitation i where i.tokenHash = :hash")
    Optional<StaffInvitation> findLocked(@Param("hash") String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from StaffInvitation i where i.id = :id")
    Optional<StaffInvitation> findLockedById(@Param("id") UUID id);
}
