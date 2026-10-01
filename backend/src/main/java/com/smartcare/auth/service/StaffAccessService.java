package com.smartcare.auth.service;

import com.smartcare.auth.domain.*;
import com.smartcare.auth.repository.*;
import com.smartcare.auth.web.*;
import com.smartcare.auth.web.StaffAccessDtos.*;
import com.smartcare.audit.service.AuditService;
import com.smartcare.common.error.*;
import com.smartcare.doctor.domain.Doctor;
import com.smartcare.doctor.repository.DoctorRepository;
import com.smartcare.hospital.repository.HospitalRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class StaffAccessService {
    private final StaffInvitationRepository invitations;
    private final UserAccountRepository users;
    private final HospitalRepository hospitals;
    private final DoctorRepository doctors;
    private final AuthService auth;
    private final AuditService audit;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    public StaffAccessService(StaffInvitationRepository invitations, UserAccountRepository users,
            HospitalRepository hospitals, DoctorRepository doctors, AuthService auth, AuditService audit, Clock clock) {
        this.invitations = invitations; this.users = users; this.hospitals = hospitals;
        this.doctors = doctors; this.auth = auth; this.audit = audit; this.clock = clock;
    }
    public static Set<Role> rolesFor(String type) {
        return switch (type) {
            case "DOCTOR" -> Set.of(Role.DOCTOR);
            case "HOSPITAL_ADMIN" -> Set.of(Role.HOSPITAL_ADMIN);
            case "OFFICE_CLERK" -> Set.of(Role.RECEPTIONIST, Role.CASHIER);
            case "RECEPTIONIST" -> Set.of(Role.RECEPTIONIST);
            case "CASHIER" -> Set.of(Role.CASHIER);
            case "LAB_TECHNICIAN" -> Set.of(Role.LAB_TECHNICIAN);
            case "BLOOD_BANK_STAFF" -> Set.of(Role.BLOOD_BANK_STAFF);
            case "AMBULANCE_DISPATCHER" -> Set.of(Role.AMBULANCE_DISPATCHER);
            default -> throw new IllegalArgumentException("Unsupported staff account type.");
        };
    }
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public InvitationIssued issue(IssueInvitation input) {
        rolesFor(input.accountType()); // Never allows a public invitation to grant SUPER_ADMIN.
        if (!hospitals.findById(input.hospitalId()).map(h -> h.isActive()).orElse(false))
            throw new NotFoundException("Active hospital was not found.");
        if (users.existsByMobileNumber(input.mobileNumber().trim()))
            throw new ConflictException("Use a new staff account mobile number; existing accounts cannot be upgraded here.");
        if (input.accountType().equals("DOCTOR")) requireDoctor(input.doctorId(), input.hospitalId());
        else if (input.doctorId() != null) throw new IllegalArgumentException("Doctor profile is only valid for doctor invitations.");
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiry = clock.instant().plus(Duration.ofHours(24));
        var saved = invitations.save(new StaffInvitation(hash(code), input.mobileNumber().trim(),
                input.accountType(), input.hospitalId(), input.doctorId(), expiry));
        audit.record("STAFF_INVITATION_CREATED", "STAFF_INVITATION", saved.getId(), input.hospitalId());
        return new InvitationIssued(saved.getId(), code, expiry);
    }
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void revoke(UUID id) {
        var invitation = invitations.findLockedById(id).orElseThrow(() -> new NotFoundException("Invitation was not found."));
        invitation.revoke(clock.instant());
        audit.record("STAFF_INVITATION_REVOKED", "STAFF_INVITATION", id, invitation.getHospitalId());
    }
    @Transactional
    public AuthResponse enroll(StaffRegistration input) {
        var invitation = invitations.findLocked(hash(input.invitationCode().trim()))
                .orElseThrow(() -> new AccessDeniedException("Invalid or unavailable staff invitation."));
        if (!invitation.usable(clock.instant()) || !invitation.getMobileNumber().equals(input.patient().mobileNumber().trim())
                || !invitation.getAccountType().equals(input.accountType()))
            throw new AccessDeniedException("Invalid or unavailable staff invitation.");
        if (!hospitals.findById(invitation.getHospitalId()).map(h -> h.isActive()).orElse(false))
            throw new AccessDeniedException("The invited hospital is unavailable.");
        Doctor doctor = invitation.getDoctorId() == null ? null : requireDoctor(invitation.getDoctorId(), invitation.getHospitalId());
        var registered = auth.register(input.patient());
        var account = users.findById(registered.user().id()).orElseThrow();
        rolesFor(invitation.getAccountType()).forEach(account::grantRole);
        account.assignHospital(invitation.getHospitalId());
        if (doctor != null) doctor.linkAccount(account);
        invitation.consume(clock.instant());
        audit.recordAs(account.getId().toString(), "STAFF_INVITATION_ACCEPTED", "STAFF_INVITATION", invitation.getId(), invitation.getHospitalId());
        return auth.response(account, registered.user().patientNumber());
    }
    private Doctor requireDoctor(UUID id, UUID hospitalId) {
        if (id == null) throw new IllegalArgumentException("A doctor invitation needs an existing verified doctor profile.");
        Doctor doctor = doctors.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Doctor was not found."));
        if (!doctor.isActive() || !doctor.getHospital().getId().equals(hospitalId) || doctor.getLinkedUser() != null)
            throw new ConflictException("Doctor must be active, unlinked and belong to the invited hospital.");
        return doctor;
    }
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void assignHospital(UUID userId, UUID hospitalId, boolean assigned) {
        var account = users.findById(userId).orElseThrow(() -> new NotFoundException("Account was not found."));
        if (account.getRoles().stream().noneMatch(r -> r != Role.PATIENT && r != Role.SUPER_ADMIN))
            throw new IllegalArgumentException("An existing staff role is required; membership cannot grant a role.");
        if (!hospitals.existsById(hospitalId)) throw new NotFoundException("Hospital was not found.");
        if (assigned) account.assignHospital(hospitalId); else account.removeHospital(hospitalId);
        audit.record(assigned ? "STAFF_HOSPITAL_ASSIGNED" : "STAFF_HOSPITAL_REMOVED", "USER", userId, hospitalId);
    }
    public record StaffAccount(UUID id, String displayName, String mobileNumber, Set<Role> roles, Set<UUID> hospitalIds) {}
    @Transactional(readOnly=true)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<StaffAccount> directory() {
        return users.findAll().stream().filter(u -> u.getRoles().stream().anyMatch(r -> r != Role.PATIENT))
                .map(u -> new StaffAccount(u.getId(),u.getDisplayName(),u.getMobileNumber(),Set.copyOf(u.getRoles()),Set.copyOf(u.getHospitalIds())))
                .toList();
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
