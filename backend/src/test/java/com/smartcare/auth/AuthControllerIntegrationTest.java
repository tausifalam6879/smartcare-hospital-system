package com.smartcare.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;
    @Autowired com.smartcare.hospital.service.HospitalService hospitals;

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles="SUPER_ADMIN")
    void publicRequestHasNoPrivilegesAndAdministratorCanReviewThenActivateStaff() throws Exception {
        var hospital = hospitals.create(new com.smartcare.hospital.web.HospitalDtos.HospitalRequest(
                "REQUEST-HTTP", "Request Test Hospital", "Test street", "Delhi", "Delhi", "110001", "+919000008000", "Asia/Kolkata", true));
        var input = Map.of("name", "Test Clerk", "mobileNumber", "+919000008001", "accountType", "OFFICE_CLERK",
                "hospitalId", hospital.id(), "message", "Desk staff reference");
        mvc.perform(post("/api/v1/auth/staff-access-requests")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(input)))
                .andExpect(status().isAccepted());
        mvc.perform(get("/api/v1/auth/staff-access-requests").param("hospitalId", hospital.id().toString())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous()))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/staff-access-requests").param("hospitalId", hospital.id().toString())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("patient").roles("PATIENT")))
                .andExpect(status().isForbidden());
        String pendingJson = mvc.perform(get("/api/v1/auth/staff-access-requests").param("hospitalId", hospital.id().toString())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String requestId = objectMapper.readTree(pendingJson).get("content").get(0).get("id").asText();
        String issuedJson = mvc.perform(post("/api/v1/auth/staff-invitations")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("SUPER_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("mobileNumber", "+919000008001", "accountType", "OFFICE_CLERK",
                                "hospitalId", hospital.id(), "requestId", requestId))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.invitationCode").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(issuedJson).get("invitationCode").asText();
        mvc.perform(get("/api/v1/auth/staff-invitations").param("hospitalId", hospital.id().toString())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("SUPER_ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].invitationCode").doesNotExist())
                .andExpect(jsonPath("$.content[0].tokenHash").doesNotExist());
        mvc.perform(post("/api/v1/auth/register-staff")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(Map.of(
                                "invitationCode", code, "accountType", "OFFICE_CLERK", "patient", Map.of(
                                        "name", "Test Clerk", "mobileNumber", "+919000008001", "password", "test-clerk-password", "preferredLanguage", "en")))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.user.roles", org.hamcrest.Matchers.hasItems("RECEPTIONIST", "CASHIER")));
        mvc.perform(post("/api/v1/auth/login")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsBytes(Map.of(
                                "credential", "+919000008001", "password", "test-clerk-password", "accountType", "OFFICE_CLERK"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void patientCanRegisterLoginAndUseIssuedJwt() throws Exception {
        Map<String, Object> registration = Map.of(
                "mobileNumber", "+919876543210",
                "email", "patient@example.com",
                "name", "Asha Rao",
                "password", "safe-test-password",
                "dateOfBirth", "1992-06-15",
                "preferredLanguage", "hi"
        );

        String registrationJson = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(registration)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.roles[0]").value("PATIENT"))
                .andExpect(jsonPath("$.user.patientNumber").value(org.hamcrest.Matchers.startsWith("RVQ-")))
                .andReturn().getResponse().getContentAsString();

        String issuedToken = objectMapper.readTree(registrationJson).get("accessToken").asText();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + issuedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Asha Rao"));

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "credential", "patient@example.com",
                                "password", "safe-test-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.displayName").value("Asha Rao"));
    }

    @Test
    void duplicateMobileNumberIsRejected() throws Exception {
        Map<String, Object> registration = Map.of(
                "mobileNumber", "+919111111111",
                "name", "Test Patient",
                "password", "safe-test-password",
                "preferredLanguage", "en"
        );
        byte[] body = objectMapper.writeValueAsBytes(registration);

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void emergencyContactMustBeAMobileNumberAndReturnsFieldSpecificFeedback() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "mobileNumber", "+919222222222",
                                "name", "Test Patient",
                                "password", "safe-test-password",
                                "emergencyContact", "MD TAUSIF ALAM",
                                "preferredLanguage", "en"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.emergencyContact")
                        .value("must contain a mobile number, not a person's name"));
    }

    @Test
    void localIpFrontendOriginIsAllowedByCors() throws Exception {
        mvc.perform(options("/api/v1/auth/register")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-correlation-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"));
    }

    @Test
    void forgedPublicRoleDoesNotCreateAdminAndAnonymousCannotIssueInvitations() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("mobileNumber", "+919000008001", "name", "Public User",
                                "password", "unique-test-password", "roles", java.util.List.of("SUPER_ADMIN"), "accountType", "DOCTOR"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.roles.length()").value(1))
                .andExpect(jsonPath("$.user.roles[0]").value("PATIENT"));
        mvc.perform(post("/api/v1/auth/staff-invitations").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/register-staff").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("invitationCode", "fake", "accountType", "HOSPITAL_ADMIN",
                                "patient", Map.of("mobileNumber", "+919000008002", "name", "Fake Staff", "password", "unique-test-password")))))
                .andExpect(status().isForbidden());
    }
}
