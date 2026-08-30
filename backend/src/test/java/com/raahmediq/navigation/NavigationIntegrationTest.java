package com.raahmediq.navigation;

import com.raahmediq.appointment.domain.PaymentMethod;
import com.raahmediq.appointment.service.AppointmentService;
import com.raahmediq.appointment.web.AppointmentDtos.BookingRequest;
import com.raahmediq.auth.service.AuthService;
import com.raahmediq.auth.web.AuthResponse;
import com.raahmediq.auth.web.RegisterRequest;
import com.raahmediq.doctor.service.DoctorService;
import com.raahmediq.doctor.web.DoctorDtos.DoctorRequest;
import com.raahmediq.doctor.web.DoctorDtos.ScheduleRequest;
import com.raahmediq.hospital.service.HospitalService;
import com.raahmediq.hospital.web.HospitalDtos.DepartmentRequest;
import com.raahmediq.hospital.web.HospitalDtos.HospitalRequest;
import com.raahmediq.navigation.domain.LocationType;
import com.raahmediq.navigation.service.NavigationService;
import com.raahmediq.navigation.web.NavigationDtos.CheckpointRequest;
import com.raahmediq.navigation.web.NavigationDtos.LocationRequest;
import com.raahmediq.navigation.web.NavigationDtos.PathRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NavigationIntegrationTest {

    @Autowired NavigationService navigation;
    @Autowired HospitalService hospitals;
    @Autowired DoctorService doctors;
    @Autowired AppointmentService appointments;
    @Autowired AuthService auth;
    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(roles = "HOSPITAL_ADMIN")
    void routesUseVerifiedGraphAndStepFreePreferenceInsteadOfGuessing() throws Exception {
        var hospital = createHospital("RMQ-NAV-1");
        createLocation(hospital.id(), "ENTRY", LocationType.ENTRANCE, "Ground Floor", null, 5, 80);
        createLocation(hospital.id(), "STAIRS_G", LocationType.STAIRS, "Ground Floor", null, 30, 80);
        createLocation(hospital.id(), "STAIRS_1", LocationType.STAIRS, "1st Floor", null, 30, 40);
        createLocation(hospital.id(), "LIFT_G", LocationType.LIFT, "Ground Floor", null, 55, 80);
        createLocation(hospital.id(), "LIFT_1", LocationType.LIFT, "1st Floor", null, 55, 40);
        createLocation(hospital.id(), "ROOM_12", LocationType.DOCTOR_ROOM, "1st Floor", "OPD 12", 85, 40);
        createLocation(hospital.id(), "ISOLATED", LocationType.LAB, "Ground Floor", "LAB X", 90, 90);

        path(hospital.id(), "ENTRY", "STAIRS_G", 5, true, "Walk to stairs", "सीढ़ियों तक जाएँ");
        path(hospital.id(), "STAIRS_G", "STAIRS_1", 5, false, "Take the stairs", "सीढ़ियों से ऊपर जाएँ");
        path(hospital.id(), "STAIRS_1", "ROOM_12", 5, true, "Walk to room 12", "कमरा 12 तक जाएँ");
        path(hospital.id(), "ENTRY", "LIFT_G", 15, true, "Walk to the lift", "लिफ्ट तक जाएँ");
        path(hospital.id(), "LIFT_G", "LIFT_1", 20, true, "Take the lift", "लिफ्ट से ऊपर जाएँ");
        path(hospital.id(), "LIFT_1", "ROOM_12", 15, true, "Walk to room 12", "कमरा 12 तक जाएँ");
        navigation.createCheckpoint(hospital.id(), new CheckpointRequest("ENTRY", "RMQ-NAV-ENTRY",
                "Entrance QR", "प्रवेश QR"));

        var quickest = navigation.route(hospital.id(), "RMQ-NAV-ENTRY", "ROOM_12", "en", false);
        assertThat(quickest.available()).isTrue();
        assertThat(quickest.pathCodes()).containsExactly("ENTRY", "STAIRS_G", "STAIRS_1", "ROOM_12");
        assertThat(quickest.steps()).anyMatch(step -> !step.stepFree());

        var accessibleHindi = navigation.route(hospital.id(), "RMQ-NAV-ENTRY", "ROOM_12", "hi-IN", true);
        assertThat(accessibleHindi.pathCodes()).containsExactly("ENTRY", "LIFT_G", "LIFT_1", "ROOM_12");
        assertThat(accessibleHindi.steps()).allMatch(step -> step.stepFree());
        assertThat(accessibleHindi.steps()).extracting(step -> step.instruction()).anyMatch(text -> text.contains("लिफ्ट"));
        assertThat(accessibleHindi.safetyNotice()).contains("GPS");

        var unavailable = navigation.route(hospital.id(), "RMQ-NAV-ENTRY", "ISOLATED", "en", true);
        assertThat(unavailable.available()).isFalse();
        assertThat(unavailable.steps()).isEmpty();
        assertThat(unavailable.message()).contains("verified route");

        mvc.perform(get("/api/v1/navigation/hospitals/{hospitalId}/route", hospital.id())
                        .param("fromCheckpoint", "RMQ-NAV-ENTRY")
                        .param("destinationCode", "ROOM_12")
                        .param("language", "en")
                        .param("stepFree", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.destination.code").value("ROOM_12"))
                .andExpect(jsonPath("$.steps[1].stepFree").value(true));

        assertThatThrownBy(() -> navigation.route(hospital.id(), "UNKNOWN-QR", "ROOM_12", "en", true))
                .hasMessageContaining("not recognized");
    }

    @Test
    @WithMockUser(roles = "HOSPITAL_ADMIN")
    void appointmentDestinationRequiresOwnershipAndExactVerifiedRoom() {
        LocalDate visitDate = LocalDate.now();
        var hospital = createHospital("RMQ-NAV-2");
        var department = hospitals.createDepartment(hospital.id(),
                new DepartmentRequest("MED", "Medicine", "Navigation test"));
        var doctor = doctors.create(new DoctorRequest(hospital.id(), department.id(), "Dr. Route Test",
                "Medicine", "RMQ-NAV-REG-2", new BigDecimal("600.00"), 15, 12,
                "Care Block", "1st Floor", "OPD 12", true));
        doctors.addSchedule(doctor.id(), new ScheduleRequest(visitDate.getDayOfWeek(),
                LocalTime.of(0, 1), LocalTime.of(23, 59), 15, 12));
        createLocation(hospital.id(), "ROOM_12", LocationType.DOCTOR_ROOM, "1st Floor", "OPD 12", 80, 30);
        AuthResponse owner = patient("31", "nav.owner");
        AuthResponse other = patient("32", "nav.other");
        var appointment = appointments.book(owner.user().id(), "nav-appointment-1",
                new BookingRequest(doctor.id(), visitDate, PaymentMethod.CASH));

        var destination = navigation.appointmentDestination(owner.user().id(), appointment.id());
        assertThat(destination.destination().code()).isEqualTo("ROOM_12");
        assertThat(destination.exactRoomMatch()).isTrue();
        assertThat(destination.doctorName()).isEqualTo("Dr. Route Test");
        assertThatThrownBy(() -> navigation.appointmentDestination(other.user().id(), appointment.id()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "HOSPITAL_ADMIN")
    void appointmentDestinationFallsBackToVerifiedHelpDeskInsteadOfReturningBlankMap() {
        LocalDate visitDate = LocalDate.now();
        var hospital = createHospital("RMQ-NAV-3");
        var department = hospitals.createDepartment(hospital.id(),
                new DepartmentRequest("NEU", "Neurology", "Fallback navigation test"));
        var doctor = doctors.create(new DoctorRequest(hospital.id(), department.id(), "Dr. Unmapped Room",
                "Neurology", "RMQ-NAV-REG-3", new BigDecimal("800.00"), 15, 12,
                "Neuro Block", "2nd Floor", "OPD 99", true));
        doctors.addSchedule(doctor.id(), new ScheduleRequest(visitDate.getDayOfWeek(),
                LocalTime.of(0, 1), LocalTime.of(23, 59), 15, 12));
        createLocation(hospital.id(), "HELP_DESK", LocationType.RECEPTION, "Ground Floor", null, 45, 55);
        AuthResponse owner = patient("33", "nav.fallback");
        var appointment = appointments.book(owner.user().id(), "nav-appointment-fallback",
                new BookingRequest(doctor.id(), visitDate, PaymentMethod.CASH));

        var destination = navigation.appointmentDestination(owner.user().id(), appointment.id());

        assertThat(destination.destination().code()).isEqualTo("HELP_DESK");
        assertThat(destination.exactRoomMatch()).isFalse();
        assertThat(destination.guidanceHi()).contains("सहायता डेस्क");
    }

    private com.raahmediq.hospital.web.HospitalDtos.HospitalResponse createHospital(String code) {
        return hospitals.create(new HospitalRequest(code, "Navigation Test Hospital", "1 Map Road", "Delhi",
                "Delhi", "110001", "+911112345689", "Asia/Kolkata", true));
    }

    private void createLocation(java.util.UUID hospitalId, String code, LocationType type, String floor,
                                String room, int x, int y) {
        navigation.createLocation(hospitalId, new LocationRequest(code, code.replace('_', ' '),
                "सत्यापित स्थान", type, "Care Block", floor, "Test Zone", room, x, y));
    }

    private void path(java.util.UUID hospitalId, String from, String to, int seconds, boolean stepFree,
                      String english, String hindi) {
        navigation.createPath(hospitalId, new PathRequest(from, to, english, hindi,
                "Return: " + english, "वापस: " + hindi, 10, seconds, stepFree));
    }

    private AuthResponse patient(String digits, String label) {
        return auth.register(new RegisterRequest("+9191000000" + digits, label + "@example.com",
                "Navigation Patient", "safe-test-password", LocalDate.of(1990, 1, 1), null,
                null, null, "en"));
    }
}
