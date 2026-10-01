package com.smartcare.auth.repository;

import com.smartcare.auth.domain.StaffInvitation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from StaffInvitation i where i.tokenHash = :hash")
    Optional<StaffInvitation> findLocked(@Param("hash") String hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from StaffInvitation i where i.id = :id")
    Optional<StaffInvitation> findLockedById(@Param("id") UUID id);
}
