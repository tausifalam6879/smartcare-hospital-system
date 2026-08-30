package com.raahmediq.payment.repository;

import com.raahmediq.payment.domain.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import com.raahmediq.payment.domain.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByAppointmentId(UUID appointmentId);
    Optional<Payment> findByPatientIdAndIdempotencyKey(UUID patientId, String idempotencyKey);
    List<Payment> findAllByPatientIdOrderByCreatedAtDesc(UUID patientId);
    List<Payment> findAllByStatusAndExpiresAtBefore(PaymentStatus status, Instant instant);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.appointment.id = :appointmentId")
    Optional<Payment> findByAppointmentIdForUpdate(@Param("appointmentId") UUID appointmentId);
}
