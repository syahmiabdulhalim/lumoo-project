package com.example.lumoo.service;
import com.example.lumoo.domain.pdpp.AuditLog;
import com.example.lumoo.domain.pdpp.AuditLogRepository;
import com.example.lumoo.domain.pdpp.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.Principal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {
    @Mock private AuditLogRepository auditLogRepository;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private AuditService auditService;
    @Test
    void log_simple_savesAuditLogWithAllFields() {
        auditService.log("UPDATE", "Product", "42", "admin@test.com");
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("UPDATE", saved.getAction());
        assertEquals("Product", saved.getEntityType());
        assertEquals("42", saved.getEntityId());
        assertEquals("admin@test.com", saved.getActorId());
    }
    @Test
    void log_withRequest_setsIpAndUserAgent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("TestBrowser/1.0");
        when(request.getUserPrincipal()).thenReturn(null);
        auditService.log("VIEW", "Order", "7", null, null, request);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("10.0.0.1", saved.getIpAddress());
        assertEquals("TestBrowser/1.0", saved.getUserAgent());
        assertEquals("anonymous", saved.getActorId());
    }
    @Test
    void log_withXForwardedFor_usesFirstIp() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.2");
        when(request.getHeader("User-Agent")).thenReturn(null);
        when(request.getUserPrincipal()).thenReturn(null);
        auditService.log("LOGIN", "User", "1", null, null, request);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("203.0.113.1", captor.getValue().getIpAddress());
    }
    @Test
    void log_withPrincipal_setsActorId() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Principal principal = () -> "vendor@test.com";
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn(null);
        when(request.getUserPrincipal()).thenReturn(principal);
        auditService.log("DELETE", "Product", "5", null, null, request);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("vendor@test.com", captor.getValue().getActorId());
    }
    @Test
    void log_withBeforeAndAfter_serialisesToJson() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn(null);
        when(request.getUserPrincipal()).thenReturn(null);
        auditService.log("UPDATE", "Product", "10",
                Map.of("status", "PENDING"),
                Map.of("status", "APPROVED"),
                request);
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertNotNull(saved.getBeforeState());
        assertNotNull(saved.getAfterState());
        assertTrue(saved.getBeforeState().contains("PENDING"));
        assertTrue(saved.getAfterState().contains("APPROVED"));
    }
    @Test
    void log_withNullRequest_stillSaves() {
        auditService.log("ACTION", "Type", "id", null, null, null);
        verify(auditLogRepository).save(any(AuditLog.class));
    }
}
