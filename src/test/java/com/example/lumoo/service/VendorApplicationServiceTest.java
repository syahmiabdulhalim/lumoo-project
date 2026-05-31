package com.example.lumoo.service;
import com.example.lumoo.domain.user.*;
import com.example.lumoo.domain.vendor.VendorApplication;
import com.example.lumoo.domain.vendor.VendorApplicationRepository;
import com.example.lumoo.domain.vendor.VendorApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class VendorApplicationServiceTest {
    @Mock private VendorApplicationRepository applicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private com.example.lumoo.infrastructure.email.EmailService emailService;
    @InjectMocks private VendorApplicationService service;
    private User user;
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("vendor@test.com");
        user.setRole(Role.USER);
    }
    @Test
    void hasAlreadyApplied_returnsTrue_whenPendingApplicationExists() {
        when(applicationRepository.existsByUserAndStatus(user, "PENDING")).thenReturn(true);
        assertTrue(service.hasAlreadyApplied(user));
    }
    @Test
    void hasAlreadyApplied_returnsFalse_whenNoPendingApplication() {
        when(applicationRepository.existsByUserAndStatus(user, "PENDING")).thenReturn(false);
        assertFalse(service.hasAlreadyApplied(user));
    }
    @Test
    void canReapply_true_whenNoApplications() {
        when(applicationRepository.findByUserOrderByAppliedAtDesc(user)).thenReturn(List.of());
        assertTrue(service.canReapply(user));
    }
    @Test
    void canReapply_false_whenLatestIsPending() {
        VendorApplication app = application("PENDING", null);
        when(applicationRepository.findByUserOrderByAppliedAtDesc(user)).thenReturn(List.of(app));
        assertFalse(service.canReapply(user));
    }
    @Test
    void canReapply_false_whenRejectedWithin12Hours() {
        VendorApplication app = application("REJECTED", LocalDateTime.now().minusHours(6));
        when(applicationRepository.findByUserOrderByAppliedAtDesc(user)).thenReturn(List.of(app));
        assertFalse(service.canReapply(user));
    }
    @Test
    void canReapply_true_whenRejectedMoreThan12HoursAgo() {
        VendorApplication app = application("REJECTED", LocalDateTime.now().minusHours(13));
        when(applicationRepository.findByUserOrderByAppliedAtDesc(user)).thenReturn(List.of(app));
        assertTrue(service.canReapply(user));
    }
    @Test
    void canReapply_true_whenRejectedWithNullReviewedAt() {
        VendorApplication app = application("REJECTED", null);
        when(applicationRepository.findByUserOrderByAppliedAtDesc(user)).thenReturn(List.of(app));
        assertTrue(service.canReapply(user));
    }
    @Test
    void apply_setsUserAndSaves() {
        VendorApplication app = new VendorApplication();
        service.apply(user, app);
        assertEquals(user, app.getUser());
        verify(applicationRepository).save(app);
    }
    @Test
    void approve_setsApprovedStatus_upgradesUserToVendor() {
        VendorApplication app = application("PENDING", null);
        app.setUser(user);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        service.approve(1L);
        assertEquals("APPROVED", app.getStatus());
        assertNotNull(app.getReviewedAt());
        assertEquals(Role.VENDOR, user.getRole());
        verify(userRepository).save(user);
        verify(notificationRepository).save(any(Notification.class));
    }
    @Test
    void approve_doesNothing_whenNotFound() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());
        service.approve(99L);
        verify(userRepository, never()).save(any());
    }
    @Test
    void reject_setsRejectedStatus_withNote() {
        VendorApplication app = application("PENDING", null);
        app.setUser(user);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        service.reject(1L, "Incomplete documents");
        assertEquals("REJECTED", app.getStatus());
        assertEquals("Incomplete documents", app.getRejectionNote());
        assertNotNull(app.getReviewedAt());
        verify(applicationRepository).save(app);
        verify(notificationRepository).save(any(Notification.class));
    }
    @Test
    void reject_setsRejectedStatus_withoutNote() {
        VendorApplication app = application("PENDING", null);
        app.setUser(user);
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(app));
        service.reject(1L, null);
        assertEquals("REJECTED", app.getStatus());
        assertNull(app.getRejectionNote());
    }
    @Test
    void reject_doesNothing_whenNotFound() {
        when(applicationRepository.findById(99L)).thenReturn(Optional.empty());
        service.reject(99L, "reason");
        verify(applicationRepository, never()).save(any());
    }
    private VendorApplication application(String status, LocalDateTime reviewedAt) {
        VendorApplication app = new VendorApplication();
        app.setId(1L);
        app.setStatus(status);
        app.setReviewedAt(reviewedAt);
        app.setUser(user);
        return app;
    }
}
