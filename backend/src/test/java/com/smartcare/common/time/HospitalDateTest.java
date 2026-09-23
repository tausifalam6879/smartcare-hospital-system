package com.smartcare.common.time;

import com.smartcare.hospital.domain.Hospital;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.assertThat;

class HospitalDateTest {
    @Test
    void usesHospitalCalendarDayAcrossUtcMidnightBoundary() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-23T19:00:00Z"), ZoneOffset.UTC);
        Hospital india = new Hospital("DATE-IN", "India test", "Address", "Delhi", "Delhi", "110001", "+911111111111", "Asia/Kolkata");
        Hospital west = new Hospital("DATE-US", "US test", "Address", "LA", "CA", "90001", "+12125550100", "America/Los_Angeles");
        assertThat(HospitalDate.today(clock, india)).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(HospitalDate.today(clock, west)).isEqualTo(LocalDate.of(2026, 9, 23));
    }
}
