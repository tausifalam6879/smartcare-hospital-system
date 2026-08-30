package com.raahmediq.auth.repository;

import com.raahmediq.auth.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    boolean existsByMobileNumber(String mobileNumber);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select u from UserAccount u where u.mobileNumber = :credential or lower(u.email) = lower(:credential)")
    Optional<UserAccount> findByCredential(@Param("credential") String credential);
}
