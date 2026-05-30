package com.example.lumoo.domain.pdpp;
import com.example.lumoo.domain.pdpp.AuditLog;
import com.example.lumoo.domain.pdpp.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
@Service
public class AuditService {
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private ObjectMapper objectMapper;
    public void log(String action, String entityType, String entityId,
                    Map<?, ?> before, Map<?, ?> after, HttpServletRequest request) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setBeforeState(toJson(before));
        log.setAfterState(toJson(after));
        if (request != null) {
            log.setIpAddress(getClientIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            String actor = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
            log.setActorId(actor);
        }
        auditLogRepository.save(log);
    }
    public void log(String action, String entityType, String entityId, String actorId) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setActorId(actorId);
        auditLogRepository.save(log);
    }
    private String toJson(Map<?, ?> map) {
        if (map == null) return null;
        try { return objectMapper.writeValueAsString(map); }
        catch (Exception e) { return map.toString(); }
    }
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
