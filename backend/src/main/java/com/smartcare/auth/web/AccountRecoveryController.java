package com.smartcare.auth.web;
import com.smartcare.auth.service.AccountRecoveryService;
import com.smartcare.auth.security.AuthAttemptLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/auth")
public class AccountRecoveryController {
    private final AccountRecoveryService service;
    private final AuthAttemptLimiter attempts;
    public AccountRecoveryController(AccountRecoveryService service, AuthAttemptLimiter attempts) { this.service=service; this.attempts=attempts; }
    record Issue(@NotNull UUID userId, @AssertTrue boolean identityVerified) {}
    record Recover(@NotBlank @Size(max=20) String mobileNumber, @NotBlank @Size(max=100) String code, @NotBlank @Size(min=12,max=72) String newPassword) {}
    @PostMapping("/recovery-code")
    public AccountRecoveryService.Issued issue(@Valid @RequestBody Issue input) { return service.issue(input.userId()); }
    @PostMapping("/recover") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recover(@Valid @RequestBody Recover input, jakarta.servlet.http.HttpServletRequest http) {
        attempts.check("recovery-ip:"+http.getRemoteAddr());
        service.recover(input.mobileNumber(),input.code(),input.newPassword());
    }
}
