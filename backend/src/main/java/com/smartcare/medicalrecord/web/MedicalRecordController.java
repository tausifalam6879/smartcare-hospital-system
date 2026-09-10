package com.smartcare.medicalrecord.web;

import com.smartcare.medicalrecord.domain.DocumentType;
import com.smartcare.medicalrecord.service.MedicalRecordService;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.DocumentResponse;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.MedicalRecordResponse;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.VisitRecordRequest;
import com.smartcare.medicalrecord.web.MedicalRecordDtos.VisitResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medical-records")
public class MedicalRecordController {
    private final MedicalRecordService service;

    public MedicalRecordController(MedicalRecordService service) {
        this.service = service;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public MedicalRecordResponse mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/appointments/{appointmentId}/patient")
    @PreAuthorize("hasRole('DOCTOR')")
    public MedicalRecordResponse patientForAppointment(@AuthenticationPrincipal Jwt jwt,
                                                       @PathVariable UUID appointmentId) {
        return service.forAppointment(UUID.fromString(jwt.getSubject()), appointmentId);
    }

    @PostMapping("/visits")
    @PreAuthorize("hasRole('DOCTOR')")
    public VisitResponse recordVisit(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody VisitRecordRequest request) {
        return service.recordVisit(UUID.fromString(jwt.getSubject()), request);
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    public DocumentResponse upload(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam UUID hospitalId,
                                   @RequestParam DocumentType documentType,
                                   @RequestParam LocalDate documentDate,
                                   @RequestParam(required = false) String description,
                                   @RequestPart MultipartFile file) {
        return service.upload(UUID.fromString(jwt.getSubject()), hospitalId, documentType, documentDate,
                description, file);
    }

    @GetMapping("/documents/{documentId}/content")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID documentId) {
        var download = service.download(UUID.fromString(jwt.getSubject()), documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(download.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(download.content());
    }
}
