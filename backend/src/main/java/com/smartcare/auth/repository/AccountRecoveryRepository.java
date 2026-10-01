package com.smartcare.auth.repository;
import com.smartcare.auth.domain.AccountRecovery;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface AccountRecoveryRepository extends JpaRepository<AccountRecovery,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AccountRecovery r where r.tokenHash = :hash")
    Optional<AccountRecovery> findLocked(@Param("hash") String hash);
    java.util.List<AccountRecovery> findAllByUserId(UUID userId);
}
