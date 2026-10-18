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
import com.smartcare.common.web.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class StaffAccessService {
    private final StaffInvitationRepository invitations;
    private final UserAccountRepository users;
    private final HospitalRepository hospitals;
    private final DoctorRepository doctors;
    private final AuthService auth;
    private final AuditService audit;
    private final Clock clock;
    private final StaffAccessRequestRepository accessRequests;
    private final SecureRandom random = new SecureRandom();
    public StaffAccessService(StaffInvitationRepository invitations, UserAccountRepository users,
            HospitalRepository hospitals, DoctorRepository doctors, AuthService auth, AuditService audit, Clock clock,
            StaffAccessRequestRepository accessRequests) {
        this.invitations = invitations; this.users = users; this.hospitals = hospitals;
        this.doctors = doctors; this.auth = auth; this.audit = audit; this.clock = clock;
        this.accessRequests = accessRequests;
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
        // Serialize issuance within a hospital so concurrent clicks cannot issue competing codes.
        if (!hospitals.findByIdForUpdate(input.hospitalId()).map(h -> h.isActive()).orElse(false))
            throw new NotFoundException("Active hospital was not found.");
        if (input.accountType().equals("DOCTOR")) requireDoctor(input.doctorId(), input.hospitalId());
        else if (input.doctorId() != null) throw new IllegalArgumentException("Doctor profile is only valid for doctor invitations.");
        if (invitations.hasActiveInvitation(input.hospitalId(), input.mobileNumber().trim(), input.doctorId(), clock.instant()))
            throw new ConflictException("An active invitation already exists for this mobile number or doctor. "
                    + "Use the original code, or revoke the active invitation before issuing a replacement.");
        StaffAccessRequest request = input.requestId() == null ? null : accessRequests.findLockedById(input.requestId())
                .orElseThrow(() -> new NotFoundException("Staff request was not found."));
        if (request != null && (!request.getStatus().equals("PENDING")
                || !request.getHospitalId().equals(input.hospitalId())
                || !request.getMobileNumber().equals(input.mobileNumber().trim())
                || !request.getAccountType().equals(input.accountType())))
            throw new ConflictException("The invitation must match the pending request's hospital, role and mobile number.");
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiry = clock.instant().plus(Duration.ofHours(24));
        var saved = invitations.save(new StaffInvitation(hash(code), input.mobileNumber().trim(),
                input.accountType(), input.hospitalId(), input.doctorId(), expiry));
        if (request != null) request.approve(saved.getId());
        audit.record("STAFF_INVITATION_CREATED", "STAFF_INVITATION", saved.getId(), input.hospitalId());
        return new InvitationIssued(saved.getId(), code, expiry);
    }
    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void revoke(UUID id) {
        var invitation = invitations.findLockedById(id).orElseThrow(() -> new NotFoundException("Invitation was not found."));
        if (!invitation.usable(clock.instant()))
            throw new ConflictException("Only active invitations can be revoked. Refresh the invitation list.");
        invitation.revoke(clock.instant());
        audit.record("STAFF_INVITATION_REVOKED", "STAFF_INVITATION", id, invitation.getHospitalId());
    }
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public PageResponse<InvitationSummary> invitations(UUID hospitalId, int page) {
        return PageResponse.from(invitations.findAllByHospitalId(hospitalId,
                PageRequest.of(Math.max(page, 0), 20, Sort.by("createdAt").descending().and(Sort.by("id"))))
                .map(i -> new InvitationSummary(i.getId(), i.getMobileNumber(), i.getAccountType(),
                        i.getHospitalId(), i.getDoctorId(), i.getCreatedAt(), i.getExpiresAt(), i.status(clock.instant()))));
    }
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<InvitableDoctor> invitableDoctors(UUID hospitalId) {
        return doctors.findAllByHospitalIdAndActiveTrueOrderByNameAsc(hospitalId).stream()
                .filter(d -> d.getLinkedUser() == null)
                .map(d -> new InvitableDoctor(d.getId(), d.getName(), d.getRegistrationNumber())).toList();
    }
    @Transactional
    public AuthResponse enroll(StaffRegistration input) {
        var invitation = requireInvitation(input.invitationCode(), input.accountType(), input.patient().mobileNumber());
        Doctor doctor = invitation.getDoctorId() == null ? null : requireDoctor(invitation.getDoctorId(), invitation.getHospitalId());
        var registered = auth.register(input.patient());
        return accept(invitation, doctor, registered);
    }
    @Transactional
    public AuthResponse activateExisting(ExistingStaffActivation input) {
        var invitation = requireInvitation(input.invitationCode(), input.accountType(), input.mobileNumber());
        // Knowing an invited phone number is insufficient: the existing account password is required.
        var authenticated = auth.login(new LoginRequest(input.mobileNumber().trim(), input.password(), "AUTO"));
        Doctor doctor = invitation.getDoctorId() == null ? null : requireDoctor(invitation.getDoctorId(), invitation.getHospitalId());
        return accept(invitation, doctor, authenticated);
    }
    private StaffInvitation requireInvitation(String code, String accountType, String mobile) {
        var invitation = invitations.findLocked(hash(code.trim()))
                .orElseThrow(() -> new AccessDeniedException("Invalid or unavailable staff invitation."));
        if (!invitation.usable(clock.instant()) || !invitation.getMobileNumber().equals(mobile.trim())
                || !invitation.getAccountType().equals(accountType))
            throw new AccessDeniedException("Invalid or unavailable staff invitation.");
        if (!hospitals.findById(invitation.getHospitalId()).map(h -> h.isActive()).orElse(false))
            throw new AccessDeniedException("The invited hospital is unavailable.");
        return invitation;
    }
    private AuthResponse accept(StaffInvitation invitation, Doctor doctor, AuthResponse registered) {
        var account = users.findById(registered.user().id()).orElseThrow();
        if (doctor != null) {
            doctors.findByLinkedUserId(account.getId()).ifPresent(existing -> {
                throw new ConflictException("This account already has a linked doctor profile. Contact the administrator.");
            });
            doctor.linkAccount(account);
        }
        rolesFor(invitation.getAccountType()).forEach(account::grantRole);
        account.assignHospital(invitation.getHospitalId());
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
