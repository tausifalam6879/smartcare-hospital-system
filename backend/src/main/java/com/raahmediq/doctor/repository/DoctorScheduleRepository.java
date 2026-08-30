package com.raahmediq.doctor.repository;

import com.raahmediq.doctor.domain.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, UUID> {
    List<DoctorSchedule> findAllByDoctorIdOrderByDayOfWeekAscStartTimeAsc(UUID doctorId);
    List<DoctorSchedule> findAllByDoctorIdAndDayOfWeek(UUID doctorId, DayOfWeek dayOfWeek);
}
