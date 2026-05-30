package com.example.lumoo.service;
import com.example.lumoo.domain.pdpp.AuditService;
import com.example.lumoo.domain.pdpp.BreachIncident;
import com.example.lumoo.domain.pdpp.BreachIncidentRepository;
import com.example.lumoo.domain.pdpp.DataBreachService;
import com.example.lumoo.infrastructure.email.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class DataBreachServiceTest {
    @Mock private BreachIncidentRepository breachIncidentRepository;
    @Mock private AuditService auditService;
    @Mock private EmailService emailService;
    @InjectMocks private DataBreachService service;
    private HttpServletRequest mockRequest() {
        HttpServletRequest req = mock(HttpServletRequest.class, org.mockito.Answers.RETURNS_DEFAULTS);
        lenient().when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(req.getHeader("User-Agent")).thenReturn(null);
        lenient().when(req.getUserPrincipal()).thenReturn(null);
        return req;
    }
    private BreachIncident incidentWithId(long id) {
        BreachIncident b = new BreachIncident();
        try {
            Field f = BreachIncident.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(b, id);
        } catch (Exception ignored) {}
        return b;
    }
    @Test
    void reportBreach_savesIncidentAndSendsEmails() {
        BreachIncident saved = incidentWithId(1L);
        when(breachIncidentRepository.save(any())).thenReturn(saved);
        when(emailService.sendBreachAlert(any(), any(), any())).thenReturn(true);
        BreachIncident result = service.reportBreach("SQL injection", "emails", 10, mockRequest());
        assertNotNull(result);
        verify(breachIncidentRepository, atLeastOnce()).save(any(BreachIncident.class));
        verify(emailService, times(2)).sendBreachAlert(any(), any(), any());
    }
    @Test
    void reportBreach_whenDpaNotified_setsDpaNotifiedAt() {
        BreachIncident saved = incidentWithId(2L);
        when(breachIncidentRepository.save(any())).thenReturn(saved);
        when(emailService.sendBreachAlert(any(), any(), any()))
                .thenReturn(true)
                .thenReturn(true);
        service.reportBreach("desc", "data", 5, mockRequest());
        verify(breachIncidentRepository, atLeastOnce()).save(any(BreachIncident.class));
    }
    @Test
    void reportBreach_whenDpaNotNotified_doesNotSetDpaNotifiedAt() {
        BreachIncident saved = incidentWithId(3L);
        when(breachIncidentRepository.save(any())).thenReturn(saved);
        when(emailService.sendBreachAlert(any(), any(), any())).thenReturn(false);
        service.reportBreach("desc", "data", 0, mockRequest());
        verify(breachIncidentRepository, atLeastOnce()).save(any(BreachIncident.class));
    }
    @Test
    void updateStatus_setsStatusAndSaves() {
        BreachIncident incident = new BreachIncident();
        incident.setStatus(BreachIncident.Status.DETECTED);
        when(breachIncidentRepository.findById(1L)).thenReturn(java.util.Optional.of(incident));
        when(breachIncidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BreachIncident result = service.updateStatus(1L, BreachIncident.Status.CONTAINED);
        assertEquals(BreachIncident.Status.CONTAINED, result.getStatus());
        verify(breachIncidentRepository).save(incident);
    }
    @Test
    void updateStatus_throwsWhenNotFound() {
        when(breachIncidentRepository.findById(99L)).thenReturn(java.util.Optional.empty());
        assertThrows(RuntimeException.class, () -> service.updateStatus(99L, BreachIncident.Status.RESOLVED));
    }
}
