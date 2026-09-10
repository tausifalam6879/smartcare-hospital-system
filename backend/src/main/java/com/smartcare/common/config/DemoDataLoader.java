package com.smartcare.common.config;

import com.smartcare.ambulance.domain.Ambulance;
import com.smartcare.ambulance.repository.AmbulanceRepository;
import com.smartcare.auth.domain.Role;
import com.smartcare.auth.domain.UserAccount;
import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.bloodbank.domain.BloodBank;
import com.smartcare.bloodbank.domain.BloodBankSourceType;
import com.smartcare.bloodbank.domain.BloodComponent;
import com.smartcare.bloodbank.domain.BloodGroup;
import com.smartcare.bloodbank.domain.BloodInventoryBatch;
import com.smartcare.bloodbank.domain.InventoryVerificationStatus;
import com.smartcare.bloodbank.repository.BloodBankRepository;
import com.smartcare.bloodbank.repository.BloodInventoryBatchRepository;
import com.smartcare.doctor.domain.Doctor;
import com.smartcare.doctor.domain.DoctorSchedule;
import com.smartcare.doctor.repository.DoctorRepository;
import com.smartcare.doctor.repository.DoctorScheduleRepository;
import com.smartcare.diagnostic.domain.DiagnosticModality;
import com.smartcare.diagnostic.domain.DiagnosticProcedure;
import com.smartcare.diagnostic.repository.DiagnosticProcedureRepository;
import com.smartcare.hospital.domain.Department;
import com.smartcare.hospital.domain.Hospital;
import com.smartcare.hospital.repository.DepartmentRepository;
import com.smartcare.hospital.repository.HospitalRepository;
import com.smartcare.navigation.domain.HospitalLocation;
import com.smartcare.navigation.domain.LocationType;
import com.smartcare.navigation.domain.NavigationPath;
import com.smartcare.navigation.domain.QrCheckpoint;
import com.smartcare.navigation.repository.HospitalLocationRepository;
import com.smartcare.navigation.repository.NavigationPathRepository;
import com.smartcare.navigation.repository.QrCheckpointRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "smartcare.demo-data", name = "enabled", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    private final HospitalRepository hospitals;
    private final DepartmentRepository departments;
    private final DoctorRepository doctors;
    private final DoctorScheduleRepository schedules;
    private final HospitalLocationRepository locations;
    private final NavigationPathRepository navigationPaths;
    private final QrCheckpointRepository checkpoints;
    private final DiagnosticProcedureRepository diagnosticProcedures;
    private final BloodBankRepository bloodBanks;
    private final BloodInventoryBatchRepository bloodInventory;
    private final AmbulanceRepository ambulances;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoDataLoader(HospitalRepository hospitals, DepartmentRepository departments,
                          DoctorRepository doctors, DoctorScheduleRepository schedules,
                          HospitalLocationRepository locations, NavigationPathRepository navigationPaths,
                          QrCheckpointRepository checkpoints,
                          DiagnosticProcedureRepository diagnosticProcedures,
                          BloodBankRepository bloodBanks, BloodInventoryBatchRepository bloodInventory,
                          AmbulanceRepository ambulances, UserAccountRepository users,
                          PasswordEncoder passwordEncoder, Clock clock) {
        this.hospitals = hospitals;
        this.departments = departments;
        this.doctors = doctors;
        this.schedules = schedules;
        this.locations = locations;
        this.navigationPaths = navigationPaths;
        this.checkpoints = checkpoints;
        this.diagnosticProcedures = diagnosticProcedures;
        this.bloodBanks = bloodBanks;
        this.bloodInventory = bloodInventory;
        this.ambulances = ambulances;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Hospital hospital = hospitals.findByCodeIgnoreCase("SC-DEMO")
                .or(() -> hospitals.findFirstByNameIgnoreCase("SmartCare Demo Care Centre"))
                .orElseGet(() -> hospitals.save(new Hospital("SC-DEMO", "SmartCare Demo Care Centre",
                        "18 Community Health Avenue", "New Delhi", "Delhi", "110001", "+911140404040",
                        "Asia/Kolkata")));
        hospital.update("SC-DEMO", "SmartCare Demo Care Centre",
                "18 Community Health Avenue", "New Delhi", "Delhi", "110001", "+911140404040",
                "Asia/Kolkata", true);
        seedStaffAccounts();
        seedCareDirectory(hospital);
        deactivateDuplicateDemoDoctors(hospital);

        // This seeder is intentionally idempotent. It upgrades older demo databases with newly mapped
        // departments instead of leaving existing installations with only the first two OPD rooms.
        seedNavigation(hospital);
        seedDiagnostics(hospital);
        seedBloodSupport(hospital);
        seedAmbulances(hospital);
    }

    private void seedStaffAccounts() {
        demoStaff("+919999990201", "Demo Ambulance Dispatcher", "DemoDispatch@2026",
                Role.AMBULANCE_DISPATCHER);
        demoStaff("+919999990202", "Demo Lab Technician", "DemoLab@2026", Role.LAB_TECHNICIAN);
        demoStaff("+919999990203", "Demo Blood Bank Staff", "DemoBlood@2026", Role.BLOOD_BANK_STAFF);
        demoStaff("+919999990204", "Demo Hospital Administrator", "DemoAdmin@2026", Role.HOSPITAL_ADMIN);
    }

    private void demoStaff(String mobile, String displayName, String password, Role role) {
        users.findByCredential(mobile).orElseGet(() -> users.save(new UserAccount(mobile, null,
                passwordEncoder.encode(password), displayName, "en", Set.of(role))));
    }

    private void seedCareDirectory(Hospital hospital) {
        Department medicine = demoDepartment(hospital, "GEN-MED", "General Medicine",
                "Adult primary and internal medicine care");
        Department cardiology = demoDepartment(hospital, "CARD", "Cardiology", "Heart and vascular care");
        Department neurology = demoDepartment(hospital, "NEURO", "Neurology", "Brain, nerve and movement care");
        Department orthopaedics = demoDepartment(hospital, "ORTHO", "Orthopaedics", "Bone, joint and mobility care");
        Department paediatrics = demoDepartment(hospital, "PAEDS", "Paediatrics", "Child and adolescent care");
        Department gynaecology = demoDepartment(hospital, "OBGYN", "Obstetrics & Gynaecology", "Women's health and maternity care");
        Department ent = demoDepartment(hospital, "ENT", "ENT", "Ear, nose and throat care");
        Department dermatology = demoDepartment(hospital, "DERM", "Dermatology", "Skin, hair and nail care");
        Department oncology = demoDepartment(hospital, "ONCO", "Oncology", "Multidisciplinary cancer care");
        Department ophthalmology = demoDepartment(hospital, "OPHTH", "Ophthalmology", "Eye and vision care");

        demoDoctor(hospital, medicine, "Dr. Ananya Mehta", "Internal Medicine", "SC-DMC-1042",
                "700.00", 15, 36, "Aarogya Block", "2nd Floor", "OPD 214", 9, 14);
        demoDoctor(hospital, cardiology, "Dr. Arjun Rao", "Cardiology", "SC-DMC-2087",
                "950.00", 20, 28, "Hriday Block", "3rd Floor", "OPD 307", 10, 15);
        demoDoctor(hospital, neurology, "Dr. Kavya Nair", "Neurology", "SC-DEMO-NEU-301",
                "1100.00", 20, 24, "Neuro Block", "2nd Floor", "OPD 228", 10, 14);
        demoDoctor(hospital, orthopaedics, "Dr. Rohan Singh", "Orthopaedic Surgery", "SC-DEMO-ORT-401",
                "850.00", 15, 32, "Mobility Block", "1st Floor", "OPD 118", 9, 14);
        demoDoctor(hospital, paediatrics, "Dr. Meera Iyer", "Paediatrics", "SC-DEMO-PED-501",
                "650.00", 15, 34, "Child Care Block", "1st Floor", "OPD 126", 9, 14);
        demoDoctor(hospital, gynaecology, "Dr. Sana Ahmed", "Obstetrics & Gynaecology", "SC-DEMO-OBG-601",
                "900.00", 20, 26, "Sakhi Block", "2nd Floor", "OPD 242", 10, 15);
        demoDoctor(hospital, ent, "Dr. Vivek Das", "ENT", "SC-DEMO-ENT-701",
                "700.00", 15, 30, "Speciality Block", "1st Floor", "OPD 132", 9, 13);
        demoDoctor(hospital, dermatology, "Dr. Isha Kapoor", "Dermatology", "SC-DEMO-DER-801",
                "750.00", 15, 30, "Speciality Block", "1st Floor", "OPD 136", 11, 16);
        demoDoctor(hospital, oncology, "Dr. Aditya Sen", "Medical Oncology", "SC-DEMO-ONC-901",
                "1200.00", 25, 20, "Cancer Care Block", "Ground Floor", "OPD 048", 10, 14);
        demoDoctor(hospital, ophthalmology, "Dr. Nidhi Verma", "Ophthalmology", "SC-DEMO-EYE-101",
                "700.00", 15, 32, "Vision Block", "Ground Floor", "OPD 056", 9, 14);
    }

    private Department demoDepartment(Hospital hospital, String code, String name, String description) {
        return departments.findByHospitalIdAndCodeIgnoreCase(hospital.getId(), code)
                .orElseGet(() -> departments.save(new Department(hospital, code, name, description)));
    }

    private Doctor demoDoctor(Hospital hospital, Department department, String name, String specialization,
                              String registration, String fee, int consultationMinutes, int capacity,
                              String building, String floor, String room, int startHour, int endHour) {
        Doctor doctor = doctors.findByRegistrationNumberIgnoreCase(registration)
                .or(() -> doctors.findFirstByHospitalIdAndNameIgnoreCase(hospital.getId(), name))
                .orElseGet(() -> doctors.save(new Doctor(hospital, department, name, specialization, registration,
                        new BigDecimal(fee), consultationMinutes, capacity, building, floor, room)));
        if (schedules.findAllByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctor.getId()).isEmpty()) {
            for (DayOfWeek day : DayOfWeek.values()) {
                schedules.save(new DoctorSchedule(doctor, day, LocalTime.of(startHour, 0),
                        LocalTime.of(endHour, 0), consultationMinutes, capacity));
            }
        }
        return doctor;
    }

    private void deactivateDuplicateDemoDoctors(Hospital hospital) {
        Map<String, Doctor> kept = new LinkedHashMap<>();
        for (Doctor doctor : doctors.findAllByHospitalIdAndActiveTrueOrderByNameAsc(hospital.getId())) {
            String key = doctor.getName().trim().toLowerCase();
            Doctor existing = kept.get(key);
            if (existing == null) {
                kept.put(key, doctor);
                continue;
            }
            Doctor duplicate = existing.getRegistrationNumber().startsWith("SC-") ? doctor : existing;
            if (duplicate == existing) kept.put(key, doctor);
            duplicate.update(duplicate.getDepartment(), duplicate.getName(), duplicate.getSpecialization(),
                    duplicate.getRegistrationNumber(), duplicate.getConsultationFee(),
                    duplicate.getExpectedConsultationMinutes(), duplicate.getDailyMaxCapacity(),
                    duplicate.getBuilding(), duplicate.getFloorLabel(), duplicate.getRoomNumber(), false);
        }
    }

    private void seedAmbulances(Hospital hospital) {
        demoAmbulance(hospital, "DEMO-SC-AMB-01", "DEMO ALPHA", "Synthetic demo crew A",
                "Hospital demo bay");
        demoAmbulance(hospital, "DEMO-SC-AMB-02", "DEMO BRAVO", "Synthetic demo crew B",
                "Community route demo area");
        demoAmbulance(hospital, "DEMO-SC-AMB-03", "DEMO CHARLIE", "Synthetic demo crew C",
                "Hospital demo bay");
    }

    private void demoAmbulance(Hospital hospital, String registration, String callSign,
                               String crewLabel, String area) {
        ambulances.findByRegistrationNumberIgnoreCase(registration).orElseGet(() -> ambulances.save(
                new Ambulance(hospital, registration, callSign, crewLabel, null, area, true)));
    }

    private void seedBloodSupport(Hospital hospital) {
        UserAccount verifier = users.findByCredential("+919999990099").orElseGet(() -> users.save(
                new UserAccount("+919999990099", null,
                        passwordEncoder.encode(UUID.randomUUID().toString()), "Demo Inventory Verifier", "en",
                        Set.of(Role.BLOOD_BANK_STAFF))));
        BloodBank hospitalBank = bloodBanks.findByHospitalIdAndCodeIgnoreCase(hospital.getId(), "SC-BB-MAIN")
                .orElseGet(() -> bloodBanks.save(new BloodBank(hospital, "SC-BB-MAIN",
                        "SmartCare Demo Blood Centre", "Synthetic development data — not a real facility", "+919999990100",
                        new BigDecimal("0.00"), 0, BloodBankSourceType.HOSPITAL_MANAGED)));
        BloodBank partnerBank = bloodBanks.findByHospitalIdAndCodeIgnoreCase(hospital.getId(), "SC-BB-PARTNER")
                .orElseGet(() -> bloodBanks.save(new BloodBank(hospital, "SC-BB-PARTNER",
                        "SmartCare Demo Partner Centre", "Synthetic development data — not a real facility", "+919999990101",
                        new BigDecimal("5.80"), 22, BloodBankSourceType.AUTHORIZED_PARTNER)));
        hospitalBank.updateDirectory("SmartCare Demo Blood Centre",
                "Synthetic development data — not a real facility", "+919999990100", new BigDecimal("0.00"), 0);
        partnerBank.updateDirectory("SmartCare Demo Partner Centre",
                "Synthetic development data — not a real facility", "+919999990101", new BigDecimal("5.80"), 22);
        verifyDemoBatch(hospitalBank, verifier, "DEMO-O-POS-PRBC", BloodGroup.O_POSITIVE,
                BloodComponent.PACKED_RED_CELLS, 8, 28);
        verifyDemoBatch(hospitalBank, verifier, "DEMO-O-NEG-PRBC", BloodGroup.O_NEGATIVE,
                BloodComponent.PACKED_RED_CELLS, 2, 24);
        verifyDemoBatch(hospitalBank, verifier, "DEMO-O-POS-PLT", BloodGroup.O_POSITIVE,
                BloodComponent.PLATELETS, 5, 5);
        verifyDemoBatch(partnerBank, verifier, "DEMO-O-POS-PRBC-P", BloodGroup.O_POSITIVE,
                BloodComponent.PACKED_RED_CELLS, 4, 25);
    }

    private void verifyDemoBatch(BloodBank bank, UserAccount verifier, String reference, BloodGroup group,
                                 BloodComponent component, int units, int expiresInDays) {
        Instant now = clock.instant();
        LocalDate expiry = LocalDate.now(clock).plusDays(expiresInDays);
        bloodInventory.findByBankAndBatchForUpdate(bank.getId(), reference).ifPresentOrElse(
                existing -> existing.verify(expiry, units, InventoryVerificationStatus.VERIFIED, now, verifier),
                () -> bloodInventory.save(new BloodInventoryBatch(bank, group, component, reference, expiry,
                        units, InventoryVerificationStatus.VERIFIED, now, verifier)));
    }

    private void seedDiagnostics(Hospital hospital) {
        demoDiagnostic(hospital, "LAB-CBC", "Complete Blood Count",
                DiagnosticModality.LAB, "No fasting is required. Drink water normally before collection.",
                8, 80, 10, "450.00", "Central Block", "Ground Floor", "LAB 08");
        demoDiagnostic(hospital, "LAB-HBA1C", "HbA1c",
                DiagnosticModality.LAB, "No fasting is required. Continue regular medicines unless your clinician advises otherwise.",
                24, 55, 10, "650.00", "Central Block", "Ground Floor", "LAB 08");
        demoDiagnostic(hospital, "LAB-LFT", "Liver Function Test", DiagnosticModality.LAB,
                "Fasting requirements vary. Follow the clinician order or confirm with the collection desk.",
                12, 60, 10, "850.00", "Central Block", "Ground Floor", "LAB 08");
        demoDiagnostic(hospital, "LAB-KFT", "Kidney Function Test", DiagnosticModality.LAB,
                "Drink water normally unless your clinician has given a fluid restriction.",
                12, 60, 10, "750.00", "Central Block", "Ground Floor", "LAB 08");
        demoDiagnostic(hospital, "LAB-THYROID", "Thyroid Profile", DiagnosticModality.LAB,
                "Tell the collection team about thyroid medicines and follow the ordering clinician's timing advice.",
                18, 55, 10, "900.00", "Central Block", "Ground Floor", "LAB 08");
        demoDiagnostic(hospital, "LAB-LIPID", "Lipid Profile", DiagnosticModality.LAB,
                "Fasting may be requested for some orders. Confirm the exact instruction before your visit.",
                12, 55, 10, "800.00", "Central Block", "Ground Floor", "LAB 08");
        demoDiagnostic(hospital, "LAB-URINE", "Urine Routine Examination", DiagnosticModality.LAB,
                "Use the sterile container supplied by the collection desk and follow their sample instructions.",
                8, 70, 10, "300.00", "Central Block", "Ground Floor", "LAB 09");
        demoDiagnostic(hospital, "IMG-CHEST-XR", "Chest X-ray",
                DiagnosticModality.X_RAY, "Remove jewellery and metal objects from the chest area before imaging.",
                4, 40, 15, "900.00", "Diagnostics Block", "Ground Floor", "XR 02");
        demoDiagnostic(hospital, "IMG-BRAIN-MRI", "MRI Brain",
                DiagnosticModality.MRI, "Tell the radiology team about implants, metal devices, pregnancy, or claustrophobia before the scan.",
                24, 12, 45, "6200.00", "Diagnostics Block", "Lower Ground Floor", "MRI 01");
        demoDiagnostic(hospital, "IMG-HEAD-CT", "CT Head", DiagnosticModality.CT,
                "Tell the imaging team about pregnancy, allergies and kidney problems; they will confirm whether contrast is planned.",
                8, 20, 20, "3200.00", "Diagnostics Block", "Lower Ground Floor", "CT 01");
    }

    private void demoDiagnostic(Hospital hospital, String code, String name, DiagnosticModality modality,
                                String preparation, int turnaroundHours, int capacity, int duration,
                                String fee, String building, String floor, String room) {
        if (!diagnosticProcedures.existsByHospitalIdAndCodeIgnoreCase(hospital.getId(), code)) {
            diagnosticProcedures.save(new DiagnosticProcedure(hospital, code, name, modality, preparation,
                    turnaroundHours, capacity, duration, new BigDecimal(fee), building, floor, room));
        }
    }

    private void seedNavigation(Hospital hospital) {
        Map<String, HospitalLocation> node = new LinkedHashMap<>();
        add(node, new HospitalLocation(hospital, "MAIN_ENTRANCE", "Main entrance", "मुख्य प्रवेश",
                LocationType.ENTRANCE, "Central Block", "Ground Floor", "Arrival", null, 8, 82));
        add(node, new HospitalLocation(hospital, "MAIN_REGISTRATION", "Main registration", "मुख्य पंजीकरण",
                LocationType.REGISTRATION, "Central Block", "Ground Floor", "Welcome Hall", null, 24, 72));
        add(node, new HospitalLocation(hospital, "LIFT_A_G", "Lift A · Ground floor", "लिफ्ट A · भूतल",
                LocationType.LIFT, "Central Block", "Ground Floor", "Lift Lobby A", null, 39, 69));
        add(node, new HospitalLocation(hospital, "STAIRS_A_G", "Stairs A · Ground floor", "सीढ़ियाँ A · भूतल",
                LocationType.STAIRS, "Central Block", "Ground Floor", "Lift Lobby A", null, 39, 88));
        add(node, new HospitalLocation(hospital, "LAB_G", "Sample collection lab", "नमूना संग्रह लैब",
                LocationType.LAB, "Central Block", "Ground Floor", "Diagnostics", "LAB 08", 55, 59));
        add(node, new HospitalLocation(hospital, "PHARMACY_G", "24-hour pharmacy", "24 घंटे की फार्मेसी",
                LocationType.PHARMACY, "Central Block", "Ground Floor", "Exit Lobby", "P 01", 58, 84));
        add(node, new HospitalLocation(hospital, "EMERGENCY_G", "Emergency department", "आपातकालीन विभाग",
                LocationType.EMERGENCY, "Central Block", "Ground Floor", "Red Zone", "ER", 82, 82));
        add(node, new HospitalLocation(hospital, "LIFT_A_2", "Lift A · 2nd floor", "लिफ्ट A · दूसरी मंज़िल",
                LocationType.LIFT, "Aarogya Block", "2nd Floor", "Lift Lobby A", null, 39, 39));
        add(node, new HospitalLocation(hospital, "LIFT_A_1", "Lift A · 1st floor", "लिफ्ट A · पहली मंज़िल",
                LocationType.LIFT, "Central Block", "1st Floor", "Lift Lobby A", null, 39, 54));
        add(node, new HospitalLocation(hospital, "STAIRS_A_2", "Stairs A · 2nd floor", "सीढ़ियाँ A · दूसरी मंज़िल",
                LocationType.STAIRS, "Aarogya Block", "2nd Floor", "Lift Lobby A", null, 39, 52));
        add(node, new HospitalLocation(hospital, "GENERAL_RECEPTION", "General medicine reception",
                "सामान्य चिकित्सा रिसेप्शन", LocationType.RECEPTION, "Aarogya Block", "2nd Floor",
                "Blue Zone", null, 59, 38));
        add(node, new HospitalLocation(hospital, "OPD_214", "Dr. Ananya · OPD 214", "डॉ. अनन्या · ओपीडी 214",
                LocationType.DOCTOR_ROOM, "Aarogya Block", "2nd Floor", "Blue Zone", "OPD 214", 80, 32));
        add(node, new HospitalLocation(hospital, "LIFT_B_G", "Lift B · Ground floor", "लिफ्ट B · भूतल",
                LocationType.LIFT, "Central Block", "Ground Floor", "Lift Lobby B", null, 64, 69));
        add(node, new HospitalLocation(hospital, "LIFT_B_3", "Lift B · 3rd floor", "लिफ्ट B · तीसरी मंज़िल",
                LocationType.LIFT, "Hriday Block", "3rd Floor", "Lift Lobby B", null, 64, 17));
        add(node, new HospitalLocation(hospital, "CARDIO_RECEPTION", "Cardiology reception", "हृदय रोग रिसेप्शन",
                LocationType.RECEPTION, "Hriday Block", "3rd Floor", "Green Zone", null, 78, 17));
        add(node, new HospitalLocation(hospital, "OPD_307", "Dr. Arjun · OPD 307", "डॉ. अर्जुन · ओपीडी 307",
                LocationType.DOCTOR_ROOM, "Hriday Block", "3rd Floor", "Green Zone", "OPD 307", 92, 17));
        add(node, new HospitalLocation(hospital, "FIRST_FLOOR_RECEPTION", "First-floor OPD help desk",
                "पहली मंज़िल ओपीडी सहायता डेस्क", LocationType.RECEPTION, "Central Block", "1st Floor",
                "Amber Zone", null, 55, 54));
        add(node, new HospitalLocation(hospital, "OPD_118", "Dr. Rohan · OPD 118", "डॉ. रोहन · ओपीडी 118",
                LocationType.DOCTOR_ROOM, "Mobility Block", "1st Floor", "Amber Zone", "OPD 118", 70, 48));
        add(node, new HospitalLocation(hospital, "OPD_126", "Dr. Meera · OPD 126", "डॉ. मीरा · ओपीडी 126",
                LocationType.DOCTOR_ROOM, "Child Care Block", "1st Floor", "Yellow Zone", "OPD 126", 82, 53));
        add(node, new HospitalLocation(hospital, "OPD_132", "Dr. Vivek · OPD 132", "डॉ. विवेक · ओपीडी 132",
                LocationType.DOCTOR_ROOM, "Speciality Block", "1st Floor", "Purple Zone", "OPD 132", 72, 61));
        add(node, new HospitalLocation(hospital, "OPD_136", "Dr. Isha · OPD 136", "डॉ. ईशा · ओपीडी 136",
                LocationType.DOCTOR_ROOM, "Speciality Block", "1st Floor", "Purple Zone", "OPD 136", 86, 64));
        add(node, new HospitalLocation(hospital, "NEURO_RECEPTION", "Neurology reception", "न्यूरोलॉजी रिसेप्शन",
                LocationType.RECEPTION, "Neuro Block", "2nd Floor", "Indigo Zone", null, 67, 45));
        add(node, new HospitalLocation(hospital, "OPD_228", "Dr. Kavya · OPD 228", "डॉ. काव्या · ओपीडी 228",
                LocationType.DOCTOR_ROOM, "Neuro Block", "2nd Floor", "Indigo Zone", "OPD 228", 84, 43));
        add(node, new HospitalLocation(hospital, "SAKHI_RECEPTION", "Women's health reception",
                "महिला स्वास्थ्य रिसेप्शन", LocationType.RECEPTION, "Sakhi Block", "2nd Floor",
                "Rose Zone", null, 62, 29));
        add(node, new HospitalLocation(hospital, "OPD_242", "Dr. Sana · OPD 242", "डॉ. सना · ओपीडी 242",
                LocationType.DOCTOR_ROOM, "Sakhi Block", "2nd Floor", "Rose Zone", "OPD 242", 80, 24));
        add(node, new HospitalLocation(hospital, "CANCER_RECEPTION", "Cancer care reception",
                "कैंसर देखभाल रिसेप्शन", LocationType.RECEPTION, "Cancer Care Block", "Ground Floor",
                "Crimson Zone", null, 48, 76));
        add(node, new HospitalLocation(hospital, "OPD_048", "Dr. Aditya · OPD 048", "डॉ. आदित्य · ओपीडी 048",
                LocationType.DOCTOR_ROOM, "Cancer Care Block", "Ground Floor", "Crimson Zone", "OPD 048", 69, 77));
        add(node, new HospitalLocation(hospital, "VISION_RECEPTION", "Vision care reception",
                "नेत्र देखभाल रिसेप्शन", LocationType.RECEPTION, "Vision Block", "Ground Floor",
                "Sky Zone", null, 34, 81));
        add(node, new HospitalLocation(hospital, "OPD_056", "Dr. Nidhi · OPD 056", "डॉ. निधि · ओपीडी 056",
                LocationType.DOCTOR_ROOM, "Vision Block", "Ground Floor", "Sky Zone", "OPD 056", 45, 89));

        path(hospital, node, "MAIN_ENTRANCE", "MAIN_REGISTRATION",
                "Go straight for 35 metres to the registration desk.", "35 मीटर सीधे पंजीकरण डेस्क तक जाएँ।",
                "Go straight for 35 metres to the main entrance.", "35 मीटर सीधे मुख्य प्रवेश तक जाएँ।", 35, 35, true);
        path(hospital, node, "MAIN_REGISTRATION", "LIFT_A_G",
                "From registration, follow the blue line for 28 metres to Lift A.", "पंजीकरण से नीली लाइन पर 28 मीटर चलकर लिफ्ट A तक जाएँ।",
                "Follow the blue line for 28 metres back to registration.", "नीली लाइन पर 28 मीटर चलकर पंजीकरण पर लौटें।", 28, 30, true);
        path(hospital, node, "MAIN_REGISTRATION", "STAIRS_A_G",
                "Turn left after registration and walk 32 metres to Stairs A.", "पंजीकरण के बाद बाएँ मुड़ें और 32 मीटर चलकर सीढ़ियाँ A तक जाएँ।",
                "Walk 32 metres from Stairs A to registration.", "सीढ़ियाँ A से 32 मीटर चलकर पंजीकरण तक जाएँ।", 32, 32, true);
        path(hospital, node, "LIFT_A_G", "LIFT_A_2",
                "Take Lift A to the 2nd floor.", "लिफ्ट A से दूसरी मंज़िल पर जाएँ।",
                "Take Lift A to the ground floor.", "लिफ्ट A से भूतल पर जाएँ।", 8, 55, true);
        path(hospital, node, "LIFT_A_G", "LIFT_A_1",
                "Take Lift A to the 1st floor.", "लिफ्ट A से पहली मंज़िल पर जाएँ।",
                "Take Lift A to the ground floor.", "लिफ्ट A से भूतल पर जाएँ।", 6, 42, true);
        path(hospital, node, "LIFT_A_1", "FIRST_FLOOR_RECEPTION",
                "Exit Lift A and follow the amber OPD signs for 18 metres to the help desk.",
                "लिफ्ट A से निकलकर नारंगी ओपीडी संकेतों पर 18 मीटर सहायता डेस्क तक जाएँ।",
                "Follow the amber signs for 18 metres from the help desk to Lift A.",
                "सहायता डेस्क से नारंगी संकेतों पर 18 मीटर लिफ्ट A तक जाएँ।", 18, 20, true);
        path(hospital, node, "FIRST_FLOOR_RECEPTION", "OPD_118",
                "Follow the mobility signs for 22 metres. OPD 118 is on the left.",
                "मोबिलिटी संकेतों पर 22 मीटर चलें। ओपीडी 118 बाईं ओर है।",
                "Walk 22 metres from OPD 118 to the first-floor help desk.",
                "ओपीडी 118 से 22 मीटर चलकर पहली मंज़िल सहायता डेस्क तक जाएँ।", 22, 24, true);
        path(hospital, node, "FIRST_FLOOR_RECEPTION", "OPD_126",
                "Follow the yellow child-care signs for 30 metres to OPD 126.",
                "पीले बाल देखभाल संकेतों पर 30 मीटर चलकर ओपीडी 126 जाएँ।",
                "Follow the yellow signs for 30 metres back to the help desk.",
                "पीले संकेतों पर 30 मीटर चलकर सहायता डेस्क लौटें।", 30, 34, true);
        path(hospital, node, "FIRST_FLOOR_RECEPTION", "OPD_132",
                "Turn right at the purple sign and walk 25 metres to OPD 132.",
                "बैंगनी संकेत पर दाएँ मुड़ें और 25 मीटर चलकर ओपीडी 132 जाएँ।",
                "Walk 25 metres from OPD 132 to the first-floor help desk.",
                "ओपीडी 132 से 25 मीटर चलकर पहली मंज़िल सहायता डेस्क जाएँ।", 25, 28, true);
        path(hospital, node, "OPD_132", "OPD_136",
                "Continue 16 metres along the purple corridor. OPD 136 is the next door.",
                "बैंगनी गलियारे में 16 मीटर आगे जाएँ। ओपीडी 136 अगला दरवाज़ा है।",
                "Walk 16 metres along the purple corridor to OPD 132.",
                "बैंगनी गलियारे में 16 मीटर चलकर ओपीडी 132 जाएँ।", 16, 18, true);
        path(hospital, node, "STAIRS_A_G", "STAIRS_A_2",
                "Use Stairs A to reach the 2nd floor. This route is not step-free.", "सीढ़ियाँ A से दूसरी मंज़िल पर जाएँ। यह रास्ता व्हीलचेयर के अनुकूल नहीं है।",
                "Use Stairs A to reach the ground floor. This route is not step-free.", "सीढ़ियाँ A से भूतल पर जाएँ। यह रास्ता व्हीलचेयर के अनुकूल नहीं है।", 36, 75, false);
        path(hospital, node, "LIFT_A_2", "GENERAL_RECEPTION",
                "Exit Lift A, turn right at the blue sign and walk 22 metres to reception.", "लिफ्ट A से निकलकर नीले संकेत पर दाएँ मुड़ें और 22 मीटर रिसेप्शन तक जाएँ।",
                "From reception, walk 22 metres to Lift A.", "रिसेप्शन से 22 मीटर चलकर लिफ्ट A तक जाएँ।", 22, 25, true);
        path(hospital, node, "STAIRS_A_2", "GENERAL_RECEPTION",
                "Exit the stairs, turn left and follow the blue signs to reception.", "सीढ़ियों से निकलकर बाएँ मुड़ें और नीले संकेतों से रिसेप्शन तक जाएँ।",
                "Follow the blue signs from reception to Stairs A.", "रिसेप्शन से नीले संकेतों के साथ सीढ़ियाँ A तक जाएँ।", 24, 24, true);
        path(hospital, node, "GENERAL_RECEPTION", "OPD_214",
                "Pass reception and continue 30 metres. OPD 214 is the second door on the left.", "रिसेप्शन पार करके 30 मीटर आगे जाएँ। ओपीडी 214 बाईं ओर दूसरा दरवाज़ा है।",
                "Walk 30 metres from OPD 214 to the general medicine reception.", "ओपीडी 214 से 30 मीटर चलकर सामान्य चिकित्सा रिसेप्शन तक जाएँ।", 30, 35, true);
        path(hospital, node, "GENERAL_RECEPTION", "NEURO_RECEPTION",
                "Follow the indigo brain signs for 20 metres to neurology reception.",
                "इंडिगो मस्तिष्क संकेतों पर 20 मीटर चलकर न्यूरोलॉजी रिसेप्शन जाएँ।",
                "Follow the indigo signs for 20 metres to general medicine reception.",
                "इंडिगो संकेतों पर 20 मीटर चलकर सामान्य चिकित्सा रिसेप्शन जाएँ।", 20, 23, true);
        path(hospital, node, "NEURO_RECEPTION", "OPD_228",
                "Continue 24 metres. OPD 228 is the second door on the right.",
                "24 मीटर आगे जाएँ। ओपीडी 228 दाईं ओर दूसरा दरवाज़ा है।",
                "Walk 24 metres from OPD 228 to neurology reception.",
                "ओपीडी 228 से 24 मीटर चलकर न्यूरोलॉजी रिसेप्शन जाएँ।", 24, 27, true);
        path(hospital, node, "GENERAL_RECEPTION", "SAKHI_RECEPTION",
                "Follow the rose signs for 18 metres to women's health reception.",
                "गुलाबी संकेतों पर 18 मीटर चलकर महिला स्वास्थ्य रिसेप्शन जाएँ।",
                "Follow the rose signs for 18 metres to general medicine reception.",
                "गुलाबी संकेतों पर 18 मीटर चलकर सामान्य चिकित्सा रिसेप्शन जाएँ।", 18, 21, true);
        path(hospital, node, "SAKHI_RECEPTION", "OPD_242",
                "Continue 26 metres. OPD 242 is beside the counselling room.",
                "26 मीटर आगे जाएँ। ओपीडी 242 परामर्श कक्ष के पास है।",
                "Walk 26 metres from OPD 242 to women's health reception.",
                "ओपीडी 242 से 26 मीटर चलकर महिला स्वास्थ्य रिसेप्शन जाएँ।", 26, 30, true);
        path(hospital, node, "MAIN_REGISTRATION", "LAB_G",
                "Turn right at registration and follow the orange signs for 45 metres to the lab.", "पंजीकरण पर दाएँ मुड़ें और नारंगी संकेतों पर 45 मीटर लैब तक जाएँ।",
                "Follow the orange signs for 45 metres from the lab to registration.", "लैब से नारंगी संकेतों पर 45 मीटर पंजीकरण तक जाएँ।", 45, 48, true);
        path(hospital, node, "MAIN_REGISTRATION", "CANCER_RECEPTION",
                "Follow the crimson cancer-care signs for 32 metres to reception.",
                "गहरे लाल कैंसर देखभाल संकेतों पर 32 मीटर चलकर रिसेप्शन जाएँ।",
                "Follow the crimson signs for 32 metres back to registration.",
                "गहरे लाल संकेतों पर 32 मीटर चलकर पंजीकरण लौटें।", 32, 36, true);
        path(hospital, node, "CANCER_RECEPTION", "OPD_048",
                "Continue 28 metres. OPD 048 is the first door after the counselling desk.",
                "28 मीटर आगे जाएँ। ओपीडी 048 परामर्श डेस्क के बाद पहला दरवाज़ा है।",
                "Walk 28 metres from OPD 048 to cancer care reception.",
                "ओपीडी 048 से 28 मीटर चलकर कैंसर देखभाल रिसेप्शन जाएँ।", 28, 32, true);
        path(hospital, node, "MAIN_REGISTRATION", "VISION_RECEPTION",
                "Turn left at the sky-blue sign and walk 20 metres to vision reception.",
                "आसमानी नीले संकेत पर बाएँ मुड़ें और 20 मीटर नेत्र रिसेप्शन तक जाएँ।",
                "Walk 20 metres from vision reception to registration.",
                "नेत्र रिसेप्शन से 20 मीटर चलकर पंजीकरण जाएँ।", 20, 23, true);
        path(hospital, node, "VISION_RECEPTION", "OPD_056",
                "Follow the eye symbols for 18 metres. OPD 056 is on the right.",
                "आँख के संकेतों पर 18 मीटर चलें। ओपीडी 056 दाईं ओर है।",
                "Walk 18 metres from OPD 056 to vision reception.",
                "ओपीडी 056 से 18 मीटर चलकर नेत्र रिसेप्शन जाएँ।", 18, 21, true);
        path(hospital, node, "LAB_G", "PHARMACY_G",
                "Continue along the ground-floor corridor for 30 metres to the pharmacy.", "भूतल के गलियारे में 30 मीटर आगे फार्मेसी तक जाएँ।",
                "Follow the corridor for 30 metres from the pharmacy to the lab.", "फार्मेसी से गलियारे में 30 मीटर चलकर लैब तक जाएँ।", 30, 32, true);
        path(hospital, node, "PHARMACY_G", "EMERGENCY_G",
                "Follow the red emergency signs for 40 metres.", "लाल आपातकालीन संकेतों पर 40 मीटर चलें।",
                "Follow the exit signs for 40 metres from emergency to the pharmacy.", "आपातकालीन विभाग से निकास संकेतों पर 40 मीटर फार्मेसी तक जाएँ।", 40, 42, true);
        path(hospital, node, "MAIN_REGISTRATION", "LIFT_B_G",
                "Follow the green line for 55 metres to Lift B.", "हरी लाइन पर 55 मीटर चलकर लिफ्ट B तक जाएँ।",
                "Follow the green line for 55 metres to registration.", "हरी लाइन पर 55 मीटर चलकर पंजीकरण तक जाएँ।", 55, 60, true);
        path(hospital, node, "LIFT_B_G", "LIFT_B_3",
                "Take Lift B to the 3rd floor.", "लिफ्ट B से तीसरी मंज़िल पर जाएँ।",
                "Take Lift B to the ground floor.", "लिफ्ट B से भूतल पर जाएँ।", 10, 65, true);
        path(hospital, node, "LIFT_B_3", "CARDIO_RECEPTION",
                "Exit Lift B and follow the green heart signs for 18 metres to reception.", "लिफ्ट B से निकलकर हरे दिल के संकेतों पर 18 मीटर रिसेप्शन तक जाएँ।",
                "Follow the green heart signs for 18 metres from reception to Lift B.", "रिसेप्शन से हरे दिल के संकेतों पर 18 मीटर लिफ्ट B तक जाएँ।", 18, 20, true);
        path(hospital, node, "CARDIO_RECEPTION", "OPD_307",
                "OPD 307 is 24 metres ahead, the third door on the right.", "ओपीडी 307 24 मीटर आगे दाईं ओर तीसरा दरवाज़ा है।",
                "Walk 24 metres from OPD 307 to cardiology reception.", "ओपीडी 307 से 24 मीटर चलकर हृदय रोग रिसेप्शन तक जाएँ।", 24, 28, true);

        checkpoint(hospital, node, "MAIN_ENTRANCE", "SC-DEMO-ENTRANCE", "Main entrance QR", "मुख्य प्रवेश QR");
        checkpoint(hospital, node, "MAIN_REGISTRATION", "SC-DEMO-REG", "Registration QR", "पंजीकरण QR");
        checkpoint(hospital, node, "LIFT_A_2", "SC-DEMO-LIFT-A2", "Lift A · 2nd floor QR", "लिफ्ट A · दूसरी मंज़िल QR");
        checkpoint(hospital, node, "LIFT_B_3", "SC-DEMO-LIFT-B3", "Lift B · 3rd floor QR", "लिफ्ट B · तीसरी मंज़िल QR");
        checkpoint(hospital, node, "LIFT_A_1", "SC-DEMO-LIFT-A1", "Lift A · 1st floor QR", "लिफ्ट A · पहली मंज़िल QR");
        checkpoint(hospital, node, "FIRST_FLOOR_RECEPTION", "SC-DEMO-OPD-1", "First-floor OPD help desk QR", "पहली मंज़िल ओपीडी सहायता डेस्क QR");
        checkpoint(hospital, node, "GENERAL_RECEPTION", "SC-DEMO-OPD-2", "Second-floor OPD reception QR", "दूसरी मंज़िल ओपीडी रिसेप्शन QR");
    }

    private void add(Map<String, HospitalLocation> node, HospitalLocation location) {
        HospitalLocation stored = locations.findByHospitalIdAndCodeIgnoreCase(
                location.getHospital().getId(), location.getCode()).orElseGet(() -> locations.save(location));
        node.put(stored.getCode(), stored);
    }

    private void path(Hospital hospital, Map<String, HospitalLocation> node, String from, String to,
                      String en, String hi, String reverseEn, String reverseHi,
                      int metres, int seconds, boolean stepFree) {
        boolean exists = navigationPaths.findAllByHospitalIdAndActiveTrue(hospital.getId()).stream().anyMatch(existing ->
                (existing.getFromLocation().getCode().equalsIgnoreCase(from)
                        && existing.getToLocation().getCode().equalsIgnoreCase(to))
                        || (existing.getFromLocation().getCode().equalsIgnoreCase(to)
                        && existing.getToLocation().getCode().equalsIgnoreCase(from)));
        if (!exists) {
            navigationPaths.save(new NavigationPath(hospital, node.get(from), node.get(to), en, hi, reverseEn,
                    reverseHi, metres, seconds, stepFree));
        }
    }

    private void checkpoint(Hospital hospital, Map<String, HospitalLocation> node, String locationCode,
                            String publicCode, String labelEn, String labelHi) {
        if (checkpoints.findByPublicCodeIgnoreCaseAndActiveTrue(publicCode).isEmpty()) {
            checkpoints.save(new QrCheckpoint(hospital, node.get(locationCode), publicCode, labelEn, labelHi));
        }
    }
}
