package com.smartcare;

import com.smartcare.common.config.StagingSafetyChecks;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class StagingSafetyChecksTest {
    private MockEnvironment valid() {
        return new MockEnvironment().withProperty("SMARTCARE_JWT_SECRET", "a".repeat(48))
                .withProperty("SMARTCARE_PAYMENT_WEBHOOK_SECRET", "b".repeat(48))
                .withProperty("DB_URL", "jdbc:postgresql://private-db/smartcare")
                .withProperty("DB_USERNAME", "qa").withProperty("DB_PASSWORD", "test-only")
                .withProperty("SMARTCARE_JWT_ISSUER", "smartcare-staging")
                .withProperty("SMARTCARE_DOCUMENT_STORAGE_PATH", "/private-documents")
                .withProperty("SMARTCARE_PERSISTENT_DOCUMENT_STORAGE_CONFIRMED", "true")
                .withProperty("SMARTCARE_CORS_ORIGINS", "https://staging.example.com");
    }
    @Test void acceptsExplicitStagingConfiguration() {
        assertThatCode(() -> new StagingSafetyChecks(valid()).afterPropertiesSet()).doesNotThrowAnyException();
    }
    @Test void rejectsUnsafeOrigins() {
        for (String origin : new String[]{"*", "http://example.com", "https://example.com/path", "https://user@example.com", "https://example.com,", "https://localhost"})
            assertThatThrownBy(() -> new StagingSafetyChecks(valid().withProperty("SMARTCARE_CORS_ORIGINS", origin)).afterPropertiesSet()).isInstanceOf(IllegalStateException.class);
    }
    @Test void rejectsDemoAccountsUnconfirmedStorageAndDevelopmentSecrets() {
        for (String[] pair : new String[][]{{"SMARTCARE_DEMO_DATA", "true"}, {"SMARTCARE_PERSISTENT_DOCUMENT_STORAGE_CONFIRMED", "false"}, {"SMARTCARE_JWT_SECRET", "dev-only-change-this-64-character-signing-secret-before-production-use"}, {"DB_PASSWORD", ""}})
            assertThatThrownBy(() -> new StagingSafetyChecks(valid().withProperty(pair[0], pair[1])).afterPropertiesSet()).isInstanceOf(IllegalStateException.class);
    }
}
