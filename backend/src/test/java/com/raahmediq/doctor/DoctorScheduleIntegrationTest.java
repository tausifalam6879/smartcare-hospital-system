package com.raahmediq.doctor;

import com.raahmediq.common.error.ConflictException;
import com.raahmediq.doctor.service.DoctorService;
import com.raahmediq.doctor.web.DoctorDtos.DoctorRequest;
import com.raahmediq.doctor.web.DoctorDtos.ScheduleRequest;
import com.raahmediq.hospital.service.HospitalService;
import com.raahmediq.hospital.web.HospitalDtos.DepartmentRequest;
import com.raahmediq.hospital.web.HospitalDtos.HospitalRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class DoctorScheduleIntegrationTest {

    @Autowired
    HospitalService hospitals;

    @Autowired
    DoctorService doctors;

    @Test
    void overlappingWeeklyScheduleIsRejected() {
        var hospital = hospitals.create(new HospitalRequest("MUM-CARE", "Mumbai Care Hospital",
                "2 Health Road", "Mumbai", "Maharashtra", "400001", "+912212345678",
                "Asia/Kolkata", true));
        var department = hospitals.createDepartment(hospital.id(),
                new DepartmentRequest("ONC", "Oncology", "Cancer care"));
        var doctor = doctors.create(new DoctorRequest(hospital.id(), department.id(), "Dr. Meera Shah",
                "Medical Oncology", "MMC-TEST-100", new BigDecimal("900.00"), 15, 60,
                "Building B", "3rd Floor", "Room 305", true));

        doctors.addSchedule(doctor.id(), new ScheduleRequest(DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(13, 0), 15, 60));

        assertThatThrownBy(() -> doctors.addSchedule(doctor.id(), new ScheduleRequest(DayOfWeek.MONDAY,
                LocalTime.of(12, 30), LocalTime.of(15, 0), 15, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlaps");
    }
}
