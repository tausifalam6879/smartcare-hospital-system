package com.raahmediq.payment.service;

import com.raahmediq.payment.config.PaymentProperties;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class WebhookSignatureVerifier {

    private final byte[] secret;

    public WebhookSignatureVerifier(PaymentProperties properties) {
        this.secret = properties.webhookSecret().getBytes(StandardCharsets.UTF_8);
    }

    public void verify(String body, String suppliedSignature) {
        if (suppliedSignature == null || suppliedSignature.isBlank()) {
            throw new AccessDeniedException("Payment webhook signature is required.");
        }
        byte[] expected = hmac(body);
        byte[] supplied;
        try {
            supplied = HexFormat.of().parseHex(suppliedSignature.trim());
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Payment webhook signature is invalid.");
        }
        if (!MessageDigest.isEqual(expected, supplied)) {
            throw new AccessDeniedException("Payment webhook signature is invalid.");
        }
    }

    public String payloadHash(String body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash webhook payload.", exception);
        }
    }

    private byte[] hmac(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to verify webhook signature.", exception);
        }
    }
}
