package com.smartcare.auth.service;

import com.smartcare.audit.service.AuditService;
import com.smartcare.auth.domain.Role;
import com.smartcare.auth.domain.UserAccount;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.auth.security.UserPrincipal;
import com.smartcare.auth.web.AuthResponse;
import com.smartcare.auth.web.LoginRequest;
import com.smartcare.auth.web.RegisterRequest;
import com.smartcare.common.error.ConflictException;
import com.smartcare.patient.service.PatientRegistration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final PatientRegistration patients;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final AuditService audit;

    public AuthService(UserAccountRepository users, PatientRegistration patients, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, TokenService tokenService, AuditService audit) {
        this.users = users;
        this.patients = patients;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.audit = audit;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String mobile = request.mobileNumber().trim();
        String email = normalizeEmail(request.email());
        if (users.existsByMobileNumber(mobile)) {
            throw new ConflictException("An account already uses this mobile number.");
        }
        if (email != null && users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account already uses this email address.");
        }

        UserAccount account = users.save(new UserAccount(mobile, email, passwordEncoder.encode(request.password()),
                request.name().trim(), defaultLanguage(request.preferredLanguage()), Set.of(Role.PATIENT)));
        String patientNumber = patients.createProfile(account, request.dateOfBirth(), blankToNull(request.gender()),
                blankToNull(request.emergencyContact()), blankToNull(request.address()));
        audit.recordAs(account.getId().toString(), "PATIENT_REGISTERED", "USER", account.getId(), null);
        return response(account, patientNumber);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.credential().trim(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        UserAccount account = users.findById(principal.id()).orElseThrow();
        return response(account, null);
    }

    private AuthResponse response(UserAccount account, String patientNumber) {
        TokenService.IssuedToken token = tokenService.issue(account);
        List<String> roles = account.getRoles().stream().map(Enum::name).sorted().toList();
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(),
                new AuthResponse.UserSummary(account.getId(), account.getDisplayName(), account.getMobileNumber(),
                        account.getEmail(), patientNumber, roles));
    }

    private static String normalizeEmail(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String defaultLanguage(String value) {
        return value == null || value.isBlank() ? "en" : value;
    }
}
