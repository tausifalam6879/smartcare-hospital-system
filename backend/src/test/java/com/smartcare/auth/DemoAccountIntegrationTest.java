package com.smartcare.auth;

import com.smartcare.auth.domain.Role;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.auth.service.AuthService;
import com.smartcare.auth.web.LoginRequest;
import com.smartcare.common.config.DemoDataLoader;
import com.smartcare.doctor.repository.DoctorRepository;
import com.smartcare.hospital.repository.HospitalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "smartcare.demo-data.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:demo-accounts;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
@Transactional
class DemoAccountIntegrationTest {
    @Autowired DemoDataLoader loader;
    @Autowired DoctorRepository doctors;
    @Autowired HospitalRepository hospitals;
    @Autowired UserAccountRepository users;
    @Autowired AuthService auth;

    @Test
    void allDemoDoctorsCanLoginAndRemainLinkedAfterRestart() throws Exception {
        var hospital = hospitals.findByCodeIgnoreCase("SC-DEMO").orElseThrow();
        var directory = doctors.findAllByHospitalIdAndActiveTrueOrderByNameAsc(hospital.getId());
        assertThat(directory).hasSize(10);
        for (var doctor : directory) {
            assertThat(doctor.getLinkedUser()).isNotNull();
            var login = auth.login(new LoginRequest(doctor.getLinkedUser().getMobileNumber(), "DemoDoctor@2026"));
            assertThat(login.user().roles()).containsExactly("DOCTOR");
            assertThat(doctors.findByLinkedUserId(login.user().id()).orElseThrow().getId()).isEqualTo(doctor.getId());
        }
        long count = users.count();
        loader.run(new DefaultApplicationArguments(new String[0]));
        assertThat(users.count()).isEqualTo(count);
        var clerk = auth.login(new LoginRequest("+919999990207", "DemoOffice@2026"));
        assertThat(clerk.user().roles()).containsExactlyInAnyOrder(Role.RECEPTIONIST.name(), Role.CASHIER.name());
    }
}
