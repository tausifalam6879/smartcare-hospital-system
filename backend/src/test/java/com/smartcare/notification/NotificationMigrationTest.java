package com.smartcare.notification;

import com.smartcare.notification.domain.NotificationType;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;

class NotificationMigrationTest {
    @Test void currentConstraintIncludesEveryApplicationNotificationType() throws Exception {
        try (var input = getClass().getResourceAsStream("/db/migration/V22__diagnostic_notification_types.sql")) {
            assertThat(input).isNotNull();
            var sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (var type : NotificationType.values()) {
                assertThat(sql).as("Migration must permit %s", type).contains("'" + type.name() + "'");
            }
        }
    }
}
