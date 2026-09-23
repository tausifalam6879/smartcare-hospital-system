package com.smartcare.common.time;

import com.smartcare.hospital.domain.Hospital;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** Calendar-day rules follow the hospital, while event timestamps remain UTC. */
public final class HospitalDate {
    private HospitalDate() { }

    public static LocalDate today(Clock clock, Hospital hospital) {
        return LocalDate.now(clock.withZone(ZoneId.of(hospital.getTimeZone())));
    }
}
