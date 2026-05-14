package com.example.lumoo.service;

import com.example.lumoo.model.SiteSettings;
import com.example.lumoo.repository.SiteSettingsRepository;
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
