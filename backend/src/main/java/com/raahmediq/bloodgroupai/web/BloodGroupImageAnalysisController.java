package com.raahmediq.bloodgroupai.web;

import com.raahmediq.bloodgroupai.domain.BloodGroupAnalysisStatus;
import com.raahmediq.bloodgroupai.service.BloodGroupImageAnalysisService;
import com.raahmediq.bloodgroupai.web.BloodGroupAnalysisDtos.AnalysisResponse;
import com.raahmediq.bloodgroupai.web.BloodGroupAnalysisDtos.RecordObservations;
import com.raahmediq.bloodgroupai.web.BloodGroupAnalysisDtos.RejectAnalysis;
import com.raahmediq.bloodgroupai.web.BloodGroupAnalysisDtos.VerifyAnalysis;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/blood-group-analyses")
public class BloodGroupImageAnalysisController {
    private final BloodGroupImageAnalysisService service;

    public BloodGroupImageAnalysisController(BloodGroupImageAnalysisService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    public AnalysisResponse submit(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID hospitalId,
                                   @RequestParam boolean safetyAcknowledged,
                                   @RequestPart("file") MultipartFile file) {
        return service.submit(subject(jwt), hospitalId, safetyAcknowledged, file);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PATIENT')")
    public List<AnalysisResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(subject(jwt));
    }

    @GetMapping("/worklist")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN','BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public List<AnalysisResponse> worklist(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID hospitalId,
                                           @RequestParam(required = false) BloodGroupAnalysisStatus status) {
        return service.worklist(subject(jwt), hospitalId, status);
    }

    @GetMapping("/{analysisId}/image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID analysisId) {
        var image = service.download(subject(jwt), analysisId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(image.filename(), StandardCharsets.UTF_8).build().toString())
                .body(image.content());
    }

    @PostMapping("/{analysisId}/observations")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN','BLOOD_BANK_STAFF')")
    public AnalysisResponse observations(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID analysisId,
                                         @Valid @RequestBody RecordObservations input) {
        return service.recordObservations(subject(jwt), analysisId, input);
    }

    @PostMapping("/{analysisId}/verify")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN','BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public AnalysisResponse verify(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID analysisId,
                                   @Valid @RequestBody VerifyAnalysis input) {
        return service.verify(subject(jwt), analysisId, input);
    }

    @PostMapping("/{analysisId}/reject")
    @PreAuthorize("hasAnyRole('LAB_TECHNICIAN','BLOOD_BANK_STAFF','HOSPITAL_ADMIN','SUPER_ADMIN')")
    public AnalysisResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID analysisId,
                                   @Valid @RequestBody RejectAnalysis input) {
        return service.reject(subject(jwt), analysisId, input);
    }

    private static UUID subject(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
