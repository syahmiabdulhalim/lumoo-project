package com.example.lumoo.domain.admin;
import com.example.lumoo.domain.admin.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SiteSettingsRepository extends JpaRepository<SiteSettings, Long> {
}
