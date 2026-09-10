package com.smartcare.diagnostic.service;

import com.smartcare.appointment.domain.Appointment;
import com.smartcare.appointment.domain.AppointmentStatus;
import com.smartcare.appointment.repository.AppointmentRepository;
import com.smartcare.audit.service.AuditService;
import com.smartcare.auth.domain.Role;
import com.smartcare.auth.domain.UserAccount;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.common.error.ConflictException;
import com.smartcare.common.error.NotFoundException;
import com.smartcare.diagnostic.domain.DiagnosticDayLedger;
import com.smartcare.diagnostic.domain.DiagnosticModality;
import com.smartcare.diagnostic.domain.DiagnosticOrder;
import com.smartcare.diagnostic.domain.DiagnosticOrderStatus;
import com.smartcare.diagnostic.domain.DiagnosticProcedure;
import com.smartcare.diagnostic.domain.DiagnosticResult;
import com.smartcare.diagnostic.domain.DiagnosticResultItem;
import com.smartcare.diagnostic.repository.DiagnosticDayLedgerRepository;
import com.smartcare.diagnostic.repository.DiagnosticOrderRepository;
import com.smartcare.diagnostic.repository.DiagnosticProcedureRepository;
import com.smartcare.diagnostic.repository.DiagnosticResultItemRepository;
import com.smartcare.diagnostic.repository.DiagnosticResultRepository;
import com.smartcare.diagnostic.web.DiagnosticDtos.AvailabilityResponse;
import com.smartcare.diagnostic.web.DiagnosticDtos.CancelOrderRequest;
import com.smartcare.diagnostic.web.DiagnosticDtos.CreateOrderRequest;
import com.smartcare.diagnostic.web.DiagnosticDtos.OrderResponse;
import com.smartcare.diagnostic.web.DiagnosticDtos.ProcedureRequest;
import com.smartcare.diagnostic.web.DiagnosticDtos.ProcedureResponse;
import com.smartcare.diagnostic.web.DiagnosticDtos.ResultItemResponse;
import com.smartcare.diagnostic.web.DiagnosticDtos.ScheduleOrderRequest;
import com.smartcare.diagnostic.web.DiagnosticDtos.VerifiedResultResponse;
import com.smartcare.diagnostic.web.DiagnosticDtos.VerifyResultRequest;
import com.smartcare.doctor.domain.Doctor;
import com.smartcare.doctor.repository.DoctorRepository;
import com.smartcare.hospital.domain.Hospital;
import com.smartcare.hospital.repository.HospitalRepository;
import com.smartcare.patient.domain.Patient;
import com.smartcare.patient.repository.PatientRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DiagnosticWorkflowService {
    private static final Set<Role> DIAGNOSTIC_STAFF_ROLES = Set.of(
            Role.LAB_TECHNICIAN, Role.HOSPITAL_ADMIN, Role.SUPER_ADMIN);

    private final DiagnosticProcedureRepository procedures;
    private final DiagnosticDayLedgerRepository ledgers;
    private final DiagnosticOrderRepository orders;
    private final DiagnosticResultRepository results;
    private final DiagnosticResultItemRepository resultItems;
    private final HospitalRepository hospitals;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final UserAccountRepository users;
    private final AuditService audit;
    private final Clock clock;

    public DiagnosticWorkflowService(DiagnosticProcedureRepository procedures,
                                     DiagnosticDayLedgerRepository ledgers,
                                     DiagnosticOrderRepository orders,
                                     DiagnosticResultRepository results,
                                     DiagnosticResultItemRepository resultItems,
                                     HospitalRepository hospitals, PatientRepository patients,
                                     DoctorRepository doctors, AppointmentRepository appointments,
                                     UserAccountRepository users, AuditService audit, Clock clock) {
        this.procedures = procedures;
        this.ledgers = ledgers;
        this.orders = orders;
        this.results = results;
        this.resultItems = resultItems;
        this.hospitals = hospitals;
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.users = users;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ProcedureResponse> procedures(UUID hospitalId, DiagnosticModality modality) {
        requireHospital(hospitalId);
        List<DiagnosticProcedure> matches = modality == null
                ? procedures.findAllByHospitalIdAndActiveTrueOrderByNameAsc(hospitalId)
                : procedures.findAllByHospitalIdAndModalityAndActiveTrueOrderByNameAsc(hospitalId, modality);
        return matches.stream().map(DiagnosticWorkflowService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(UUID procedureId, LocalDate serviceDate) {
        DiagnosticProcedure procedure = requireActiveProcedure(procedureId);
        requireCurrentOrFutureDate(serviceDate);
        DiagnosticDayLedger ledger = ledgers.findByProcedureIdAndServiceDate(procedureId, serviceDate).orElse(null);
        int capacity = ledger == null ? procedure.getDailyCapacity() : ledger.getEffectiveCapacity();
        int reserved = ledger == null ? 0 : ledger.getActiveCount();
        return new AvailabilityResponse(procedureId, serviceDate, capacity, reserved,
                Math.max(0, capacity - reserved));
    }

    @Transactional
    public ProcedureResponse createProcedure(ProcedureRequest request) {
        Hospital hospital = requireHospital(request.hospitalId());
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (procedures.existsByHospitalIdAndCodeIgnoreCase(hospital.getId(), code)) {
            throw new ConflictException("A diagnostic procedure already uses this code in the hospital.");
        }
        DiagnosticProcedure procedure = procedures.save(new DiagnosticProcedure(hospital, code,
                request.name().trim(), request.modality(), clean(request.preparationInstructions()),
                request.turnaroundHours(), request.dailyCapacity(), request.estimatedDurationMinutes(),
                request.fee(), clean(request.building()), clean(request.floorLabel()), clean(request.roomNumber())));
        audit.record("DIAGNOSTIC_PROCEDURE_CREATED", "DIAGNOSTIC_PROCEDURE", procedure.getId(), hospital.getId());
        return toResponse(procedure);
    }

    @Transactional
    public OrderResponse createOrder(UUID doctorUserId, CreateOrderRequest request) {
        Doctor doctor = requireLinkedDoctor(doctorUserId);
        Appointment appointment = requireAppointment(request.appointmentId());
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new AccessDeniedException("This appointment is assigned to another doctor.");
        }
        if (appointment.getStatus() != AppointmentStatus.IN_CONSULTATION
                && appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ConflictException("A diagnostic order can be created only during or after consultation.");
        }
        DiagnosticProcedure procedure = requireActiveProcedure(request.procedureId());
        if (!procedure.getHospital().getId().equals(appointment.getHospital().getId())) {
            throw new ConflictException("The diagnostic procedure belongs to another hospital.");
        }
        if (orders.existsByAppointmentIdAndProcedureId(appointment.getId(), procedure.getId())) {
            throw new ConflictException("This diagnostic procedure is already ordered for the appointment.");
        }
        DiagnosticOrder order = orders.save(new DiagnosticOrder(appointment, doctor, procedure,
                request.priority(), clean(request.clinicalNote())));
        audit.record("DIAGNOSTIC_ORDER_CREATED", "DIAGNOSTIC_ORDER", order.getId(),
                appointment.getHospital().getId());
        return toResponse(order);
    }

    @Transactional
    public List<OrderResponse> mine(UUID userId) {
        Patient patient = requirePatient(userId);
        audit.record("DIAGNOSTIC_ORDERS_VIEWED", "PATIENT", patient.getId(), null);
        return orders.findAllByPatientIdOrderByCreatedAtDesc(patient.getId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse schedule(UUID userId, UUID orderId, ScheduleOrderRequest request) {
        Patient patient = requirePatient(userId);
        requireCurrentOrFutureDate(request.serviceDate());
        DiagnosticOrder order = requireOrderForUpdate(orderId);
        requireOwner(patient, order);
        if (order.getStatus() == DiagnosticOrderStatus.SCHEDULED
                && request.serviceDate().equals(order.getScheduledDate())) return toResponse(order);
        if (order.getStatus() != DiagnosticOrderStatus.ORDERED) {
            throw new ConflictException("This diagnostic order cannot be scheduled in its current state.");
        }
        DiagnosticProcedure procedure = procedures.findByIdForUpdate(order.getProcedure().getId())
                .filter(DiagnosticProcedure::isActive)
                .orElseThrow(() -> new NotFoundException("Diagnostic procedure was not found."));
        DiagnosticDayLedger ledger = ledgers.findForUpdate(procedure.getId(), request.serviceDate())
                .orElseGet(() -> ledgers.save(new DiagnosticDayLedger(
                        procedure, request.serviceDate(), procedure.getDailyCapacity())));
        Integer position = ledger.allocate();
        if (position == null) {
            throw new ConflictException("This diagnostic service is fully booked for the selected date.");
        }
        order.schedule(request.serviceDate(), position, clock.instant());
        audit.record("DIAGNOSTIC_ORDER_SCHEDULED", "DIAGNOSTIC_ORDER", order.getId(),
                procedure.getHospital().getId());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(UUID userId, UUID orderId, CancelOrderRequest request) {
        Patient patient = requirePatient(userId);
        DiagnosticOrder order = requireOrderForUpdate(orderId);
        requireOwner(patient, order);
        if (order.getStatus() != DiagnosticOrderStatus.ORDERED
                && order.getStatus() != DiagnosticOrderStatus.SCHEDULED) {
            throw new ConflictException("This diagnostic order can no longer be cancelled.");
        }
        if (order.getStatus() == DiagnosticOrderStatus.SCHEDULED) {
            procedures.findByIdForUpdate(order.getProcedure().getId())
                    .orElseThrow(() -> new NotFoundException("Diagnostic procedure was not found."));
            ledgers.findForUpdate(order.getProcedure().getId(), order.getScheduledDate())
                    .ifPresent(DiagnosticDayLedger::release);
        }
        transition(() -> order.cancel(clock.instant(), clean(request.reason()) == null
                ? "Cancelled by patient" : request.reason().trim()));
        audit.record("DIAGNOSTIC_ORDER_CANCELLED", "DIAGNOSTIC_ORDER", order.getId(),
                order.getProcedure().getHospital().getId());
        return toResponse(order);
    }

    @Transactional
    public List<OrderResponse> worklist(UUID staffUserId, UUID hospitalId, LocalDate serviceDate) {
        requireDiagnosticStaff(staffUserId);
        requireHospital(hospitalId);
        audit.record("DIAGNOSTIC_WORKLIST_VIEWED", "HOSPITAL", hospitalId, hospitalId);
        return orders.findAllByProcedureHospitalIdAndScheduledDateOrderByQueuePositionAsc(hospitalId, serviceDate)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse collect(UUID staffUserId, UUID orderId) {
        requireDiagnosticStaff(staffUserId);
        DiagnosticOrder order = requireOrderForUpdate(orderId);
        transition(() -> order.collectSample(clock.instant()));
        audit.record("DIAGNOSTIC_SAMPLE_COLLECTED", "DIAGNOSTIC_ORDER", order.getId(),
                order.getProcedure().getHospital().getId());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse start(UUID staffUserId, UUID orderId) {
        requireDiagnosticStaff(staffUserId);
        DiagnosticOrder order = requireOrderForUpdate(orderId);
        transition(() -> order.startProcessing(clock.instant()));
        audit.record("DIAGNOSTIC_PROCESSING_STARTED", "DIAGNOSTIC_ORDER", order.getId(),
                order.getProcedure().getHospital().getId());
        return toResponse(order);
    }

    @Transactional
    public OrderResponse verifyResult(UUID staffUserId, UUID orderId, VerifyResultRequest request) {
        UserAccount verifier = requireDiagnosticStaff(staffUserId);
        DiagnosticOrder order = requireOrderForUpdate(orderId);
        if (results.findByOrderId(orderId).isPresent()) {
            throw new ConflictException("A verified result already exists for this diagnostic order.");
        }
        var verifiedAt = clock.instant();
        transition(() -> order.verifyResult(verifiedAt));
        DiagnosticResult result = results.save(new DiagnosticResult(order, request.summary().trim(),
                clean(request.findings()), clean(request.impression()), request.overallFlag(), verifier, verifiedAt));
        int itemOrder = 1;
        if (request.items() != null) {
            for (var item : request.items()) {
                resultItems.save(new DiagnosticResultItem(result, itemOrder++, item.name().trim(),
                        item.value().trim(), clean(item.unit()), clean(item.referenceRange()), item.flag()));
            }
        }
        audit.record("DIAGNOSTIC_RESULT_VERIFIED", "DIAGNOSTIC_RESULT", result.getId(),
                order.getProcedure().getHospital().getId());
        return toResponse(order);
    }

    private OrderResponse toResponse(DiagnosticOrder order) {
        DiagnosticProcedure procedure = order.getProcedure();
        VerifiedResultResponse result = results.findByOrderId(order.getId()).map(item ->
                new VerifiedResultResponse(item.getId(), item.getSummary(), item.getFindings(), item.getImpression(),
                        item.getOverallFlag(), item.getVerifiedBy().getDisplayName(), item.getVerifiedAt(),
                        resultItems.findAllByResultIdOrderByItemOrderAsc(item.getId()).stream()
                                .map(value -> new ResultItemResponse(value.getName(), value.getValue(), value.getUnit(),
                                        value.getReferenceRange(), value.getFlag())).toList())).orElse(null);
        return new OrderResponse(order.getId(), order.getAppointment().getId(),
                order.getPatient().getPatientNumber(), procedure.getId(), procedure.getCode(), procedure.getName(),
                procedure.getModality(), procedure.getHospital().getName(), order.getOrderedByDoctor().getName(),
                order.getStatus(), order.getPriority(), order.getClinicalNote(),
                procedure.getPreparationInstructions(), procedure.getTurnaroundHours(), procedure.getFee(),
                order.getScheduledDate(), order.getQueuePosition(), procedure.getBuilding(), procedure.getFloorLabel(),
                procedure.getRoomNumber(), order.getCreatedAt(), order.getScheduledAt(), order.getSampleCollectedAt(),
                order.getProcessingStartedAt(), order.getResultVerifiedAt(), order.getCancellationReason(), result);
    }

    private static ProcedureResponse toResponse(DiagnosticProcedure procedure) {
        return new ProcedureResponse(procedure.getId(), procedure.getHospital().getId(),
                procedure.getHospital().getName(), procedure.getCode(), procedure.getName(), procedure.getModality(),
                procedure.getPreparationInstructions(), procedure.getTurnaroundHours(), procedure.getDailyCapacity(),
                procedure.getEstimatedDurationMinutes(), procedure.getFee(), procedure.getBuilding(),
                procedure.getFloorLabel(), procedure.getRoomNumber());
    }

    private Hospital requireHospital(UUID hospitalId) {
        return hospitals.findById(hospitalId).filter(Hospital::isActive)
                .orElseThrow(() -> new NotFoundException("Hospital was not found."));
    }

    private DiagnosticProcedure requireActiveProcedure(UUID procedureId) {
        return procedures.findById(procedureId).filter(DiagnosticProcedure::isActive)
                .orElseThrow(() -> new NotFoundException("Diagnostic procedure was not found."));
    }

    private Patient requirePatient(UUID userId) {
        return patients.findByUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("A patient profile is required."));
    }

    private Doctor requireLinkedDoctor(UUID userId) {
        return doctors.findByLinkedUserId(userId).filter(Doctor::isActive)
                .orElseThrow(() -> new AccessDeniedException("A linked active doctor profile is required."));
    }

    private Appointment requireAppointment(UUID appointmentId) {
        return appointments.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment was not found."));
    }

    private DiagnosticOrder requireOrderForUpdate(UUID orderId) {
        return orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("Diagnostic order was not found."));
    }

    private UserAccount requireDiagnosticStaff(UUID userId) {
        UserAccount user = users.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("Diagnostic staff account is unavailable."));
        if (user.getRoles().stream().noneMatch(DIAGNOSTIC_STAFF_ROLES::contains)) {
            throw new AccessDeniedException("A diagnostic staff role is required.");
        }
        return user;
    }

    private void requireCurrentOrFutureDate(LocalDate date) {
        if (date == null || date.isBefore(LocalDate.now(clock))) {
            throw new IllegalArgumentException("Diagnostic service date cannot be in the past.");
        }
    }

    private static void requireOwner(Patient patient, DiagnosticOrder order) {
        if (!order.getPatient().getId().equals(patient.getId())) {
            throw new AccessDeniedException("Diagnostic order belongs to another patient.");
        }
    }

    private static void transition(Runnable transition) {
        try {
            transition.run();
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception.getMessage());
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
