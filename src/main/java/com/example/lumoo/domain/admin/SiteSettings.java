package com.example.lumoo.domain.admin;
import jakarta.persistence.*;
@Entity
@Table(name = "site_settings")
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
    private String socialFacebook;
    private String socialInstagram;
    private String socialTwitter;
    private boolean cookieConsentEnabled = true;
    private int dataRetentionDays = 365;
    private String pdpaRegistrationNumber;
    private String dataControllerName;
    private String dataControllerEmail;
    @Column(columnDefinition = "TEXT")
    private String privacyPolicyText;
    @Column(columnDefinition = "TEXT")
    private String termsText;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankAccountName() { return bankAccountName; }
    public void setBankAccountName(String bankAccountName) { this.bankAccountName = bankAccountName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankBranch() { return bankBranch; }
    public void setBankBranch(String bankBranch) { this.bankBranch = bankBranch; }
    public String getAfrimoneyNumber() { return afrimoneyNumber; }
    public void setAfrimoneyNumber(String afrimoneyNumber) { this.afrimoneyNumber = afrimoneyNumber; }
    public String getAfrimoneyName() { return afrimoneyName; }
    public void setAfrimoneyName(String afrimoneyName) { this.afrimoneyName = afrimoneyName; }
    public String getQmoneyNumber() { return qmoneyNumber; }
    public void setQmoneyNumber(String qmoneyNumber) { this.qmoneyNumber = qmoneyNumber; }
    public String getQmoneyName() { return qmoneyName; }
    public void setQmoneyName(String qmoneyName) { this.qmoneyName = qmoneyName; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getBusinessRegNo() { return businessRegNo; }
    public void setBusinessRegNo(String businessRegNo) { this.businessRegNo = businessRegNo; }
    public String getBusinessAddress() { return businessAddress; }
    public void setBusinessAddress(String businessAddress) { this.businessAddress = businessAddress; }
    public String getBusinessEmail() { return businessEmail; }
    public void setBusinessEmail(String businessEmail) { this.businessEmail = businessEmail; }
    public String getBusinessPhone() { return businessPhone; }
    public void setBusinessPhone(String businessPhone) { this.businessPhone = businessPhone; }
    public String getBusinessHours() { return businessHours; }
    public void setBusinessHours(String businessHours) { this.businessHours = businessHours; }
    public String getBusinessWhatsapp() { return businessWhatsapp; }
    public void setBusinessWhatsapp(String businessWhatsapp) { this.businessWhatsapp = businessWhatsapp; }
    public String getCopyrightText() { return copyrightText; }
    public void setCopyrightText(String copyrightText) { this.copyrightText = copyrightText; }
    public String getSocialFacebook() { return socialFacebook; }
    public void setSocialFacebook(String socialFacebook) { this.socialFacebook = socialFacebook; }
    public String getSocialInstagram() { return socialInstagram; }
    public void setSocialInstagram(String socialInstagram) { this.socialInstagram = socialInstagram; }
    public String getSocialTwitter() { return socialTwitter; }
    public void setSocialTwitter(String socialTwitter) { this.socialTwitter = socialTwitter; }
    public boolean isCookieConsentEnabled() { return cookieConsentEnabled; }
    public void setCookieConsentEnabled(boolean cookieConsentEnabled) { this.cookieConsentEnabled = cookieConsentEnabled; }
    public int getDataRetentionDays() { return dataRetentionDays; }
    public void setDataRetentionDays(int dataRetentionDays) { this.dataRetentionDays = dataRetentionDays; }
    public String getPdpaRegistrationNumber() { return pdpaRegistrationNumber; }
    public void setPdpaRegistrationNumber(String pdpaRegistrationNumber) { this.pdpaRegistrationNumber = pdpaRegistrationNumber; }
    public String getDataControllerName() { return dataControllerName; }
    public void setDataControllerName(String dataControllerName) { this.dataControllerName = dataControllerName; }
    public String getDataControllerEmail() { return dataControllerEmail; }
    public void setDataControllerEmail(String dataControllerEmail) { this.dataControllerEmail = dataControllerEmail; }
    public String getPrivacyPolicyText() { return privacyPolicyText; }
    public void setPrivacyPolicyText(String privacyPolicyText) { this.privacyPolicyText = privacyPolicyText; }
    public String getTermsText() { return termsText; }
    public void setTermsText(String termsText) { this.termsText = termsText; }
}
