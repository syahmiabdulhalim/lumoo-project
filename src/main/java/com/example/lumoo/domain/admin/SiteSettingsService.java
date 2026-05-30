package com.example.lumoo.domain.admin;
import com.example.lumoo.domain.admin.SiteSettings;
import com.example.lumoo.domain.admin.SiteSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
public class SiteSettingsService {
    @Autowired
    private SiteSettingsRepository repo;
    public SiteSettings get() {
        return repo.findById(1L).orElseGet(() -> {
            SiteSettings s = new SiteSettings();
            s.setId(1L);
            s.setBusinessName("LUMOO Gambia");
            s.setDataControllerName("LUMOO Gambia Ltd");
            s.setCookieConsentEnabled(true);
            s.setDataRetentionDays(365);
            return repo.save(s);
        });
    }
    public SiteSettings save(SiteSettings settings) {
        settings.setId(1L);
        return repo.save(settings);
    }
}
