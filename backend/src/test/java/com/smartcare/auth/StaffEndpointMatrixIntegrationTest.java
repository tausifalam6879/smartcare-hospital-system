package com.smartcare.auth;

import com.smartcare.StaffTestIdentity;
import com.smartcare.auth.domain.Role;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.auth.service.TokenService;
import com.smartcare.hospital.service.HospitalService;
import com.smartcare.hospital.web.HospitalDtos.HospitalRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Set;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StaffEndpointMatrixIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired HospitalService hospitals;
    @Autowired UserAccountRepository users;
    @Autowired TokenService tokens;
    @Autowired com.smartcare.auth.service.AuthService auth;
    @Autowired com.smartcare.doctor.service.DoctorService doctors;
    @Autowired com.smartcare.appointment.service.AppointmentService appointments;

    @Test
    void cashCheckInAndQueueMutationsRejectOtherHospitalWithoutChangingTheVisit() throws Exception {
        var a = hospitals.create(new HospitalRequest("MUTATION-A", "Mutation A", "Test Road", "Delhi", "Delhi", "110001", "+911111114001", "Asia/Kolkata", true));
        var b = hospitals.create(new HospitalRequest("MUTATION-B", "Mutation B", "Test Road", "Delhi", "Delhi", "110001", "+911111114002", "Asia/Kolkata", true));
        var department = hospitals.createDepartment(a.id(), new com.smartcare.hospital.web.HospitalDtos.DepartmentRequest("QA", "QA Medicine", "Test only"));
        var doctor = doctors.create(new com.smartcare.doctor.web.DoctorDtos.DoctorRequest(a.id(), department.id(), "QA Doctor", "Medicine", "QA-MUTATION", java.math.BigDecimal.valueOf(500), 15, 10, "A", "1", "101", true));
        var date = LocalDate.now();
        doctors.addSchedule(doctor.id(), new com.smartcare.doctor.web.DoctorDtos.ScheduleRequest(date.getDayOfWeek(), java.time.LocalTime.MIDNIGHT, java.time.LocalTime.of(23, 59), 15, 10));
        var patient = auth.register(new com.smartcare.auth.web.RegisterRequest("+919100004001", "mutation.qa@example.com", "QA Mutation Patient", "QA-test-password", LocalDate.of(1990, 1, 1), null, null, null, "en"));
        var visit = appointments.book(patient.user().id(), "mutation-qa", new com.smartcare.appointment.web.AppointmentDtos.BookingRequest(doctor.id(), date, com.smartcare.appointment.domain.PaymentMethod.CASH));
        var foreign = StaffTestIdentity.signIn(users, b.id(), Role.CASHIER, Role.RECEPTIONIST);
        var deniedToken = tokens.issue(foreign).value();
        var cashUrl = "/api/v1/appointments/" + visit.id() + "/cash-confirmation";
        var checkInUrl = "/api/v1/check-in/appointments/" + visit.id();
        var nextUrl = "/api/v1/queues/" + doctor.id() + "/serve-next";
        mvc.perform(post(cashUrl).header("Authorization", "Bearer " + deniedToken)).andExpect(status().isForbidden());
        mvc.perform(post(checkInUrl).header("Authorization", "Bearer " + deniedToken).contentType("application/json").content("{\"channel\":\"RECEPTION_DESK\"}")).andExpect(status().isForbidden());
        mvc.perform(post(nextUrl).param("date", date.toString()).header("Authorization", "Bearer " + deniedToken)).andExpect(status().isForbidden());
        assertThat(appointments.mine(patient.user().id()).get(0).status()).isEqualTo(com.smartcare.appointment.domain.AppointmentStatus.CASH_PENDING);
        var own = StaffTestIdentity.signIn(users, a.id(), Role.CASHIER, Role.RECEPTIONIST);
        var allowedToken = tokens.issue(own).value();
        mvc.perform(post(cashUrl).header("Authorization", "Bearer " + allowedToken)).andExpect(status().isOk());
        mvc.perform(post(checkInUrl).header("Authorization", "Bearer " + allowedToken).contentType("application/json").content("{\"channel\":\"RECEPTION_DESK\"}")).andExpect(status().isCreated());
        mvc.perform(post(nextUrl).param("date", date.toString()).header("Authorization", "Bearer " + allowedToken)).andExpect(status().isOk());
        assertThat(appointments.mine(patient.user().id()).get(0).status()).isEqualTo(com.smartcare.appointment.domain.AppointmentStatus.IN_CONSULTATION);
    }

    @Test
    void staffWorklistsRequireBothRoleAndHospitalMembershipUsingIssuedTokens() throws Exception {
        var a = hospitals.create(new HospitalRequest("HTTP-SCOPE-A", "HTTP Test A", "Test Road", "Delhi", "Delhi",
                "110001", "+911111112001", "Asia/Kolkata", true));
        var b = hospitals.create(new HospitalRequest("HTTP-SCOPE-B", "HTTP Test B", "Test Road", "Delhi", "Delhi",
                "110001", "+911111112002", "Asia/Kolkata", true));
        var endpoints = java.util.Map.of(
                "/api/v1/operations/dashboard", Set.of(Role.RECEPTIONIST, Role.CASHIER, Role.HOSPITAL_ADMIN),
                "/api/v1/diagnostics/worklist", Set.of(Role.LAB_TECHNICIAN, Role.HOSPITAL_ADMIN),
                "/api/v1/blood-requests", Set.of(Role.BLOOD_BANK_STAFF, Role.HOSPITAL_ADMIN),
                "/api/v1/ambulance-requests", Set.of(Role.AMBULANCE_DISPATCHER, Role.HOSPITAL_ADMIN));
        for (var role : new Role[]{Role.PATIENT, Role.RECEPTIONIST, Role.CASHIER, Role.LAB_TECHNICIAN,
                Role.BLOOD_BANK_STAFF, Role.AMBULANCE_DISPATCHER, Role.HOSPITAL_ADMIN}) {
            var account = StaffTestIdentity.signIn(users, a.id(), role);
            var token = tokens.issue(account).value();
            for (var endpoint : endpoints.entrySet()) {
                mvc.perform(get(endpoint.getKey()).param("hospitalId", a.id().toString())
                                .param("date", LocalDate.now().toString()).header("Authorization", "Bearer " + token))
                        .andExpect(status().is(endpoint.getValue().contains(role) ? 200 : 403));
                mvc.perform(get(endpoint.getKey()).param("hospitalId", b.id().toString())
                                .param("date", LocalDate.now().toString()).header("Authorization", "Bearer " + token))
                        .andExpect(status().isForbidden());
            }
            account.removeHospital(a.id());
            for (var endpoint : endpoints.keySet()) {
                mvc.perform(get(endpoint).param("hospitalId", a.id().toString())
                                .param("date", LocalDate.now().toString()).header("Authorization", "Bearer " + token))
                        .andExpect(status().isForbidden());
            }
        }
    }
}
