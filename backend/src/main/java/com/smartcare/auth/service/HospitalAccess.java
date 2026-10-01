package com.smartcare.auth.service;

import com.smartcare.auth.domain.AccountStatus;
import com.smartcare.auth.domain.Role;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.doctor.repository.DoctorRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/** Hospital membership supplements, never replaces, each operation's role/ownership checks. */
@Service
public class HospitalAccess {
    private final UserAccountRepository users;
    private final DoctorRepository doctors;
    public HospitalAccess(UserAccountRepository users, DoctorRepository doctors) {
        this.users = users; this.doctors = doctors;
    }
    @Transactional(readOnly = true)
    public void require(UUID userId, UUID hospitalId) {
        var user = users.findById(userId).orElseThrow(() -> new AccessDeniedException("Staff account required."));
        if (user.getStatus() != AccountStatus.ACTIVE) throw new AccessDeniedException("Inactive account.");
        if (user.getRoles().contains(Role.SUPER_ADMIN)) return;
        if (user.getHospitalIds().contains(hospitalId)) return;
        // Existing verified doctor links are also explicit hospital assignments.
        if (user.getRoles().contains(Role.DOCTOR) && doctors.findByLinkedUserId(userId)
                .filter(d -> d.isActive() && d.getHospital().getId().equals(hospitalId)).isPresent()) return;
        throw new AccessDeniedException("You are not assigned to this hospital.");
    }
    public void requireCurrent(UUID hospitalId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Staff account required.");
        try { require(UUID.fromString(auth.getName()), hospitalId); }
        catch (IllegalArgumentException ex) { throw new AccessDeniedException("Staff account required."); }
    }
}
