package com.example.lumoo.service;
import com.example.lumoo.domain.admin.SiteSettings;
import com.example.lumoo.domain.admin.SiteSettingsRepository;
import com.example.lumoo.domain.admin.SiteSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class SiteSettingsServiceTest {
    @Mock private SiteSettingsRepository repo;
    @InjectMocks private SiteSettingsService service;
    @Test
    void get_returnsExistingSettings_whenFound() {
        SiteSettings existing = new SiteSettings();
        existing.setId(1L);
        existing.setBusinessName("LUMOO");
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        SiteSettings result = service.get();
        assertEquals("LUMOO", result.getBusinessName());
        verify(repo, never()).save(any());
    }
    @Test
    void get_createsDefaultSettings_whenNotFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SiteSettings result = service.get();
        assertEquals("LUMOO Gambia", result.getBusinessName());
        assertTrue(result.isCookieConsentEnabled());
        assertEquals(365, result.getDataRetentionDays());
        verify(repo).save(any(SiteSettings.class));
    }
    @Test
    void save_forcesIdToOne_andSaves() {
        SiteSettings settings = new SiteSettings();
        settings.setBusinessName("Updated");
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SiteSettings result = service.save(settings);
        assertEquals(1L, result.getId());
        verify(repo).save(settings);
    }
}
