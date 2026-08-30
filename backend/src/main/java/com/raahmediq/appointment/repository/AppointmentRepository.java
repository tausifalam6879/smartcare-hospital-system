package com.raahmediq.appointment.repository;

import com.raahmediq.appointment.domain.Appointment;
import com.raahmediq.appointment.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from Appointment appointment where appointment.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") UUID id);

    Optional<Appointment> findByPatientIdAndIdempotencyKey(UUID patientId, String idempotencyKey);

    Optional<Appointment> findFirstByPatientIdAndDoctorIdAndServiceDateAndStatusIn(
            UUID patientId, UUID doctorId, LocalDate serviceDate, Collection<AppointmentStatus> statuses);

    List<Appointment> findAllByPatientIdOrderByServiceDateDescCreatedAtDesc(UUID patientId);

    List<Appointment> findAllByStatusAndReservationExpiresAtBefore(AppointmentStatus status, Instant instant);

    List<Appointment> findAllByStatusAndCashDeadlineAtBefore(AppointmentStatus status, Instant instant);

    List<Appointment> findAllByDoctorIdAndServiceDateAndStatusInOrderByQueuePositionAsc(
            UUID doctorId, LocalDate serviceDate, Collection<AppointmentStatus> statuses);

    Optional<Appointment> findFirstByDoctorIdAndServiceDateAndStatusOrderByQueuePositionAsc(
            UUID doctorId, LocalDate serviceDate, AppointmentStatus status);

    boolean existsByPatientIdAndDoctorId(UUID patientId, UUID doctorId);

    List<Appointment> findAllByHospitalIdAndServiceDateOrderByDoctorNameAscQueuePositionAsc(
            UUID hospitalId, LocalDate serviceDate);
}
