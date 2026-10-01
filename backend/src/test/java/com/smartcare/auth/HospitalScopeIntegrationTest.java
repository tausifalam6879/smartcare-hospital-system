package com.smartcare.auth;
import com.smartcare.auth.domain.Role;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.auth.service.HospitalAccess;
import com.smartcare.hospital.service.HospitalService;
import com.smartcare.hospital.web.HospitalDtos.HospitalRequest;
import com.smartcare.operations.service.HospitalOperationsService;
import com.smartcare.diagnostic.service.DiagnosticWorkflowService;
import com.smartcare.bloodbank.service.BloodBankService;
import com.smartcare.ambulance.service.AmbulanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Transactional
class HospitalScopeIntegrationTest {
    @Autowired HospitalService hospitals;
    @Autowired UserAccountRepository users;
    @Autowired HospitalAccess access;
    @Autowired HospitalOperationsService operations;
    @Autowired DiagnosticWorkflowService diagnostics;
    @Autowired BloodBankService blood;
    @Autowired AmbulanceService ambulance;
    @Test void staffCannotSwitchHospitalAndRevokedMembershipTakesEffectImmediately() {
        var a=hospitals.create(new HospitalRequest("SCOPE-A","Scope A","Test Road","Delhi","Delhi","110001","+911111111111","Asia/Kolkata",true));
        var b=hospitals.create(new HospitalRequest("SCOPE-B","Scope B","Test Road","Delhi","Delhi","110001","+911111111112","Asia/Kolkata",true));
        var staff=com.smartcare.StaffTestIdentity.signIn(users,a.id(),Role.HOSPITAL_ADMIN);
        access.require(staff.getId(),a.id());
        assertThat(operations.dashboard(staff.getId(),a.id(),LocalDate.now()).hospitalId()).isEqualTo(a.id());
        assertThat(diagnostics.worklist(staff.getId(),a.id(),LocalDate.now())).isEmpty();
        assertThat(blood.worklist(staff.getId(),a.id(),null)).isEmpty();
        assertThat(ambulance.worklist(staff.getId(),a.id(),null)).isEmpty();
        assertThatThrownBy(() -> operations.dashboard(staff.getId(),b.id(),LocalDate.now())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> diagnostics.worklist(staff.getId(),b.id(),LocalDate.now())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> blood.worklist(staff.getId(),b.id(),null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> ambulance.worklist(staff.getId(),b.id(),null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> ambulance.fleet(staff.getId(),b.id())).isInstanceOf(AccessDeniedException.class);
        staff.removeHospital(a.id());
        assertThatThrownBy(() -> operations.dashboard(staff.getId(),a.id(),LocalDate.now())).isInstanceOf(AccessDeniedException.class);
    }
}
