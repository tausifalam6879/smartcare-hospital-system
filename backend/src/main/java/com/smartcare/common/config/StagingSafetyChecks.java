package com.smartcare.common.config;

import java.net.URI;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Fail closed for staging; never print configuration values or credentials. */
@Component
@Profile("staging")
public class StagingSafetyChecks implements InitializingBean {
    private final Environment environment;
    public StagingSafetyChecks(Environment environment) { this.environment = environment; }

    @Override public void afterPropertiesSet() {
        secret("SMARTCARE_JWT_SECRET");
        secret("SMARTCARE_PAYMENT_WEBHOOK_SECRET");
        for (String key : new String[]{"DB_URL", "DB_USERNAME", "DB_PASSWORD", "SMARTCARE_JWT_ISSUER",
                "SMARTCARE_DOCUMENT_STORAGE_PATH"}) required(key);
        if (Boolean.parseBoolean(environment.getProperty("SMARTCARE_DEMO_DATA", "false")))
            throw new IllegalStateException("Staging must not seed shared demo accounts.");
        if (!Boolean.parseBoolean(environment.getProperty("SMARTCARE_PERSISTENT_DOCUMENT_STORAGE_CONFIRMED", "false")))
            throw new IllegalStateException("Confirm persistent private-document storage before starting staging.");
        for (String origin : required("SMARTCARE_CORS_ORIGINS").split(",", -1)) {
            try {
                URI uri = URI.create(origin.trim());
                if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                        || uri.getQuery() != null || uri.getFragment() != null
                        || (uri.getPath() != null && !uri.getPath().isEmpty())
                        || uri.getHost().equals("localhost") || uri.getHost().equals("127.0.0.1"))
                    throw new IllegalArgumentException();
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("Staging CORS requires explicit HTTPS origins without paths or wildcards.");
            }
        }
    }
    private String required(String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing staging configuration: " + key);
        return value;
    }
    private void secret(String key) {
        String value = required(key);
        if (value.length() < 32 || value.toLowerCase(java.util.Locale.ROOT).contains("dev-only")
                || value.toLowerCase(java.util.Locale.ROOT).contains("change-me"))
            throw new IllegalStateException("Replace development or short staging secret: " + key);
    }
}
