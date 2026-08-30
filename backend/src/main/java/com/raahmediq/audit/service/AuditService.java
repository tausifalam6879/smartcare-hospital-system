package com.raahmediq.audit.service;

import com.raahmediq.audit.domain.AuditLog;
import com.raahmediq.audit.repository.AuditLogRepository;
import com.raahmediq.common.error.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final Clock clock;

    public AuditService(AuditLogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void record(String action, String resourceType, UUID resourceId, UUID hospitalId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication == null ? "SYSTEM" : authentication.getName();
        repository.save(new AuditLog(actor, action, resourceType, resourceId, "SUCCESS", hospitalId,
                correlationId(), clock.instant()));
    }

    public void recordAs(String actor, String action, String resourceType, UUID resourceId, UUID hospitalId) {
        repository.save(new AuditLog(actor, action, resourceType, resourceId, "SUCCESS", hospitalId,
                correlationId(), clock.instant()));
    }

    private String correlationId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
            return value == null ? null : value.toString();
        }
        return null;
    }
}
