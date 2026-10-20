package com.example.lumoo.domain.admin;
import com.example.lumoo.infrastructure.web.HtmlSanitizer;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.*;
@Entity
@Table(name = "site_settings")
@Getter
@Setter
public class SiteSettings {
    @Id
    private Long id = 1L;
    private String bankName;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankBranch;
    private String afrimoneyNumber;
    private String afrimoneyName;
    private String qmoneyNumber;
    private String qmoneyName;
    private String businessName;
    private String businessRegNo;
    private String businessAddress;
    private String businessEmail;
    private String businessPhone;
    private String businessHours;
    private String businessWhatsapp;
    private String copyrightText;
    private String googleAnalyticsId;
    private String socialFacebook;
    private String socialInstagram;
    private String socialTwitter;
    private boolean cookieConsentEnabled = true;
    private int dataRetentionDays = 365;
    private int payoutHoldDays = 3;
    private String pdpaRegistrationNumber;
    private String dataControllerName;
    private String dataControllerEmail;
    @Column(columnDefinition = "TEXT")
    private String privacyPolicyText;
    @Column(columnDefinition = "TEXT")
    private String termsText;
    @Column(columnDefinition = "TEXT")
    private String returnsText;
    @Column(columnDefinition = "TEXT")
    private String cookiePolicyText;
    // Sanitised views for th:utext (admin-authored HTML). Field access, so JPA ignores these getters.
    public String getSafePrivacyPolicyText() { return HtmlSanitizer.sanitize(privacyPolicyText); }
    public String getSafeTermsText() { return HtmlSanitizer.sanitize(termsText); }
    public String getSafeReturnsText() { return HtmlSanitizer.sanitize(returnsText); }
    public String getSafeCookiePolicyText() { return HtmlSanitizer.sanitize(cookiePolicyText); }
}
