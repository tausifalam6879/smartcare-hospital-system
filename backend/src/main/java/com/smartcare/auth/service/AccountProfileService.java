package com.smartcare.auth.service;

import com.smartcare.auth.repository.UserAccountRepository;
import com.smartcare.patient.repository.PatientRepository;
import com.smartcare.audit.service.AuditService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

@Service
public class AccountProfileService {
    private final UserAccountRepository users;
    private final PatientRepository patients;
    private final PasswordEncoder passwords;
    private final AuditService audit;
    public AccountProfileService(UserAccountRepository users, PatientRepository patients, PasswordEncoder passwords, AuditService audit) {
        this.users = users; this.patients = patients; this.passwords = passwords; this.audit = audit;
    }
    public record Profile(String displayName, String gender, String photo, Set<UUID> hospitalIds) {}
    @Transactional(readOnly = true)
    public Profile get(UUID id) {
        var user = users.findById(id).orElseThrow();
        return new Profile(user.getDisplayName(), patients.findByUserId(id).map(p -> p.getGender()).orElse(null),
                user.getProfilePhoto(), Set.copyOf(user.getHospitalIds()));
    }
    @Transactional
    public Profile update(UUID id, String name, String gender) {
        var user = users.findById(id).orElseThrow();
        if (name == null || name.isBlank() || name.trim().length() > 120) throw new IllegalArgumentException("Name must contain 1–120 characters.");
        if (gender != null && !Set.of("MALE", "FEMALE", "OTHER", "UNDISCLOSED").contains(gender)) throw new IllegalArgumentException("Invalid gender selection.");
        user.setDisplayName(name.trim());
        patients.findByUserId(id).ifPresent(p -> p.setGender(gender));
        audit.record("PROFILE_UPDATED", "USER", id, null);
        return get(id);
    }
    @Transactional
    public Profile photo(UUID id, byte[] bytes) {
        if (bytes.length == 0 || bytes.length > 1_048_576) throw new IllegalArgumentException("Upload a JPEG or PNG photo under 1 MB.");
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Invalid photo.");
            var reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (!Set.of("jpeg", "png").contains(format) || width < 1 || height < 1 || width > 4096 || height > 4096 || (long) width * height > 12_000_000)
                    throw new IllegalArgumentException("Use a JPEG/PNG photo up to 4096 pixels and 12 megapixels.");
                BufferedImage source = reader.read(0);
                BufferedImage thumbnail = new BufferedImage(192, 192, BufferedImage.TYPE_INT_RGB);
                var graphics = thumbnail.createGraphics();
                try {
                    graphics.setColor(java.awt.Color.WHITE); graphics.fillRect(0, 0, 192, 192);
                    graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    int edge = Math.min(width, height), x = (width - edge) / 2, y = (height - edge) / 2;
                    graphics.drawImage(source, 0, 0, 192, 192, x, y, x + edge, y + edge, null);
                } finally { graphics.dispose(); }
                var output = new ByteArrayOutputStream();
                ImageIO.write(thumbnail, "png", output); // Re-encode to remove metadata and non-image content.
                users.findById(id).orElseThrow().setProfilePhoto("data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray()));
            } finally { reader.dispose(); }
        } catch (IOException ex) { throw new IllegalArgumentException("Cannot read this photo."); }
        audit.record("PROFILE_PHOTO_UPDATED", "USER", id, null);
        return get(id);
    }
    @Transactional
    public void deletePhoto(UUID id) { users.findById(id).orElseThrow().setProfilePhoto(null); audit.record("PROFILE_PHOTO_REMOVED", "USER", id, null); }
    @Transactional
    public void changePassword(UUID id, String current, String next) {
        var user = users.findById(id).orElseThrow();
        if (!passwords.matches(current, user.getPasswordHash())) throw new AccessDeniedException("Current password is incorrect.");
        if (next == null || next.length() < 12 || next.length() > 72 || next.equals(current)) throw new IllegalArgumentException("Choose a different password of 12–72 characters.");
        user.changePassword(passwords.encode(next));
        audit.record("PASSWORD_CHANGED_SESSIONS_REVOKED", "USER", id, null);
    }
    @Transactional
    public void revoke(UUID id) { users.findById(id).orElseThrow().revokeSessions(); audit.record("ALL_SESSIONS_REVOKED", "USER", id, null); }
}
