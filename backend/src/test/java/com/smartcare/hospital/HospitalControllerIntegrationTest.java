package com.smartcare.hospital;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HospitalControllerIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void directoryIsPublicButCreationRequiresAdminRole() throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(Map.of(
                "code", "DEL-CENTRAL",
                "name", "City Central Hospital",
                "addressLine", "1 Care Avenue",
                "city", "New Delhi",
                "state", "Delhi",
                "postalCode", "110001",
                "contactNumber", "+911123456789",
                "timeZone", "Asia/Kolkata"
        ));

        mvc.perform(post("/api/v1/hospitals").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/hospitals")
                        .with(jwt().jwt(token -> token.subject("admin-test"))
                                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DEL-CENTRAL"));

        mvc.perform(get("/api/v1/hospitals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("City Central Hospital"));
    }
}
