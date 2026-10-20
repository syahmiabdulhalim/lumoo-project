package com.example.lumoo.service;
import com.example.lumoo.domain.pdpp.AuditService;
import com.example.lumoo.domain.pdpp.BreachIncident;
import com.example.lumoo.domain.pdpp.BreachIncidentRepository;
import com.example.lumoo.domain.pdpp.DataBreachService;
import com.example.lumoo.infrastructure.email.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class DataBreachServiceTest {
    @Mock private BreachIncidentRepository breachIncidentRepository;
    @Mock private AuditService auditService;
    @Mock private EmailService emailService;
    @InjectMocks private DataBreachService service;
    private final HttpServletRequest req = mock(HttpServletRequest.class);
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "dpaEmail", "regulator@example.gm");
        ReflectionTestUtils.setField(service, "adminEmail", "admin@lumoo.my");
        ReflectionTestUtils.setField(service, "dpaEnabled", true);
    }
    private BreachIncident incident(long id) {
        BreachIncident b = new BreachIncident();
        ReflectionTestUtils.setField(b, "id", id);
        b.setDescription("Leaked <script>alert(1)</script> export");
        b.setDiscoveredAt(LocalDateTime.now().minusHours(1));
        return b;
    }
    @Test
    void reportBreach_alertsAdminOnly_neverTheRegulator() {
        when(breachIncidentRepository.save(any())).thenAnswer(inv -> { BreachIncident b = inv.getArgument(0); ReflectionTestUtils.setField(b, "id", 7L); return b; });
        BreachIncident b = service.reportBreach("desc", "emails", 10, LocalDateTime.now(), "rotated keys", "admin@lumoo.my", req);
        verify(emailService).sendBreachAlert(eq("admin@lumoo.my"), any(), any());
        verify(emailService, never()).sendBreachAlert(eq("regulator@example.gm"), any(), any());
        assertNull(b.getDpaNotifiedAt());
        assertEquals("admin@lumoo.my", b.getReportedBy());
    }
    @Test
    void notifyRegulator_sendsOnce_andRecordsTime() {
        BreachIncident b = incident(1);
        when(breachIncidentRepository.findById(1L)).thenReturn(Optional.of(b));
        when(emailService.sendBreachAlert(eq("regulator@example.gm"), any(), any())).thenReturn(true);
        assertEquals(DataBreachService.NotifyResult.SENT, service.notifyRegulator(1L, "admin", req));
        assertNotNull(b.getDpaNotifiedAt());
        assertEquals(DataBreachService.NotifyResult.ALREADY_SENT, service.notifyRegulator(1L, "admin", req));
        verify(emailService, times(1)).sendBreachAlert(eq("regulator@example.gm"), any(), any());
    }
    @Test
    void notifyRegulator_disabledUntilAddressConfirmed() {
        ReflectionTestUtils.setField(service, "dpaEnabled", false);
        when(breachIncidentRepository.findById(1L)).thenReturn(Optional.of(incident(1)));
        assertEquals(DataBreachService.NotifyResult.DISABLED, service.notifyRegulator(1L, "admin", req));
        verifyNoInteractions(emailService);
    }
    @Test
    void notifyRegulator_sendFailure_doesNotMarkNotified() {
        BreachIncident b = incident(1);
        when(breachIncidentRepository.findById(1L)).thenReturn(Optional.of(b));
        when(emailService.sendBreachAlert(any(), any(), any())).thenReturn(false);
        assertEquals(DataBreachService.NotifyResult.SEND_FAILED, service.notifyRegulator(1L, "admin", req));
        assertNull(b.getDpaNotifiedAt());
    }
    @Test
    void regulatorEmail_isEscaped_andHasNoInventedClaims() {
        String html = service.buildRegulatorEmail(incident(1));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertFalse(html.contains("isolated and contained"));
        assertFalse(html.contains("investigation commenced"));
    }
    @Test
    void deadline_is72hAfterDiscovery_andOverdueFlag() {
        BreachIncident b = incident(1);
        b.setDiscoveredAt(LocalDateTime.now().minusHours(80));
        assertTrue(b.isRegulatorOverdue());
        b.setDpaNotifiedAt(LocalDateTime.now());
        assertFalse(b.isRegulatorOverdue());
    }
}
