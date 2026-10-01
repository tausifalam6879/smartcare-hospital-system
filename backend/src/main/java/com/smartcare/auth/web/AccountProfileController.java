package com.smartcare.auth.web;

import com.smartcare.auth.service.AccountProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account")
public class AccountProfileController {
    private final AccountProfileService service;
    public AccountProfileController(AccountProfileService service) { this.service = service; }
    record Identity(@NotBlank @Size(max=120) String displayName, String gender) {}
    record Password(@NotBlank @Size(max=72) String currentPassword, @NotBlank @Size(min=12,max=72) String newPassword) {}
    @GetMapping
    public ResponseEntity<AccountProfileService.Profile> get(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get(id(jwt)));
    }
    @PutMapping
    public AccountProfileService.Profile update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Identity body) { return service.update(id(jwt), body.displayName(), body.gender()); }
    @PostMapping(value="/photo", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public AccountProfileService.Profile photo(@AuthenticationPrincipal Jwt jwt, @RequestParam MultipartFile file) throws IOException {
        if (file.getSize() > 1_048_576) throw new IllegalArgumentException("Photo must be under 1 MB.");
        return service.photo(id(jwt), file.getBytes());
    }
    @DeleteMapping("/photo") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@AuthenticationPrincipal Jwt jwt) { service.deletePhoto(id(jwt)); }
    @PostMapping("/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody Password body) { service.changePassword(id(jwt), body.currentPassword(), body.newPassword()); }
    @PostMapping("/logout-all") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Jwt jwt) { service.revoke(id(jwt)); }
    private UUID id(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
