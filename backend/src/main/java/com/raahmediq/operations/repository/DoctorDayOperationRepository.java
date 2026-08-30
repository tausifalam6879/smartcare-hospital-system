package com.raahmediq.operations.repository;

import com.raahmediq.operations.domain.DoctorDayOperation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorDayOperationRepository extends JpaRepository<DoctorDayOperation, UUID> {
    Optional<DoctorDayOperation> findByDoctorIdAndServiceDate(UUID doctorId, LocalDate serviceDate);
    List<DoctorDayOperation> findAllByHospitalIdAndServiceDateOrderByDoctorNameAsc(UUID hospitalId,
                                                                                  LocalDate serviceDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select item from DoctorDayOperation item where item.doctor.id = :doctorId and item.serviceDate = :date")
    Optional<DoctorDayOperation> findForUpdate(@Param("doctorId") UUID doctorId, @Param("date") LocalDate date);
}
