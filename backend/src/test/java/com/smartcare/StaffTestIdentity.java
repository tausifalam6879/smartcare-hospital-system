package com.smartcare;

import com.smartcare.auth.domain.*;
import com.smartcare.auth.repository.UserAccountRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;

public final class StaffTestIdentity {
    private StaffTestIdentity() {}
    public static UserAccount signIn(UserAccountRepository users, UUID hospitalId, Role... roles) {
        var account = users.save(new UserAccount("test-" + UUID.randomUUID().toString().substring(0, 12), null,
                "not-a-login-password", "Isolated test staff", "en", Set.of(roles)));
        account.assignHospital(hospitalId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                account.getId().toString(), "", account.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).toList()));
        return account;
    }
}
