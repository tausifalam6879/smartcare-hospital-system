package com.smartcare.auth.config;

import com.smartcare.audit.service.AuditService;
import com.smartcare.auth.domain.Role;
import com.smartcare.auth.domain.UserAccount;
import com.smartcare.auth.repository.UserAccountRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.Set;

@Configuration
public class AdminBootstrapConfig {

    @Bean
    @ConditionalOnProperty(prefix = "smartcare.bootstrap.admin", name = "enabled", havingValue = "true")
    ApplicationRunner bootstrapAdmin(AdminBootstrapProperties properties, UserAccountRepository users,
                                     PasswordEncoder encoder, AuditService audit, TransactionTemplate transactions) {
        return arguments -> transactions.executeWithoutResult(status -> {
            if (properties.mobileNumber() == null || properties.password() == null
                    || properties.password().length() < 12) {
                throw new IllegalStateException("Admin bootstrap requires a mobile number and a 12+ character password.");
            }
            if (users.existsByMobileNumber(properties.mobileNumber())) {
                return;
            }
            String email = properties.email() == null ? null : properties.email().trim().toLowerCase(Locale.ROOT);
            String name = properties.displayName() == null || properties.displayName().isBlank()
                    ? "SmartCare Administrator"
                    : properties.displayName().trim();
            UserAccount account = users.save(new UserAccount(properties.mobileNumber().trim(), email,
                    encoder.encode(properties.password()), name, "en", Set.of(Role.SUPER_ADMIN)));
            audit.recordAs("SYSTEM", "SUPER_ADMIN_BOOTSTRAPPED", "USER", account.getId(), null);
        });
    }
}
