package com.example.lumoo.controller;

import com.example.lumoo.model.Role;
import com.example.lumoo.model.User;
import com.example.lumoo.model.VendorApplication;
import com.example.lumoo.service.UserService;
import com.example.lumoo.service.VendorApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class VendorApplicationController {

    @Autowired private VendorApplicationService vendorApplicationService;
    @Autowired private UserService userService;

    @GetMapping("/buyer/vendorapply")
    public String applyPage(Model model, Principal principal) {
        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.USER) return "redirect:/?error=not_eligible";

        List<VendorApplication> applications = vendorApplicationService.getByUser(user);
        VendorApplication latest = applications.isEmpty() ? null : applications.get(0);

        String state = "FORM";
        LocalDateTime canReapplyAt = null;

        if (latest != null) {
            if ("PENDING".equals(latest.getStatus())) {
                state = "PENDING";
            } else if ("REJECTED".equals(latest.getStatus())) {
                if (latest.getReviewedAt() != null &&
                        latest.getReviewedAt().plusHours(12).isAfter(LocalDateTime.now())) {
                    state = "REJECTED_COOLDOWN";
                    canReapplyAt = latest.getReviewedAt().plusHours(12);
                }
            }
        }

        model.addAttribute("applications", applications);
        model.addAttribute("state", state);
        model.addAttribute("latest", latest);
        model.addAttribute("canReapplyAt", canReapplyAt);
        return "vendor-apply";
    }

    @PostMapping("/buyer/vendorapply")
    public String submitApplication(
            // Section 1: Business Information
            @RequestParam String businessName,
            @RequestParam String businessType,
            @RequestParam(required = false) String businessRegNumber,
            @RequestParam(required = false) String tinNumber,
            @RequestParam String yearsInBusiness,
            @RequestParam String estimatedMonthlyTurnover,
            // Section 2: Business Location
            @RequestParam String businessAddress,
            @RequestParam String businessRegion,
            @RequestParam String phone,
            // Section 3: Owner / Beneficial Owner
            @RequestParam String ownerFullName,
            @RequestParam String ownerIdType,
            @RequestParam String ownerIdNumber,
            @RequestParam String ownerAddress,
            // Section 4: Banking
            @RequestParam String bankName,
            @RequestParam String bankAccountNumber,
            @RequestParam String bankAccountName,
            @RequestParam(required = false) String mobileMoneyNumber,
            // Section 5: Products & Operations
            @RequestParam String productsToSell,
            @RequestParam String reason,
            // Section 6: Documents
            @RequestParam(required = false) MultipartFile nationalIdDoc,
            @RequestParam(required = false) MultipartFile businessRegDoc,
            Principal principal) {

        if (principal == null) return "redirect:/login";
        User user = userService.findByEmail(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";
        if (user.getRole() != Role.USER) return "redirect:/?error=not_eligible";
        if (!vendorApplicationService.canReapply(user)) return "redirect:/buyer/vendorapply";

        if (isBlank(businessName) || isBlank(businessType) || isBlank(phone)
                || isBlank(ownerFullName) || isBlank(ownerIdNumber)
                || isBlank(bankAccountNumber) || isBlank(productsToSell) || isBlank(reason)) {
            return "redirect:/buyer/vendorapply?error";
        }

        VendorApplication app = new VendorApplication();
        app.setBusinessName(businessName.trim());
        app.setBusinessType(businessType.trim());
        app.setBusinessRegNumber(trimOrNull(businessRegNumber));
        app.setTinNumber(trimOrNull(tinNumber));
        app.setYearsInBusiness(yearsInBusiness.trim());
        app.setEstimatedMonthlyTurnover(estimatedMonthlyTurnover.trim());
        app.setBusinessAddress(businessAddress.trim());
        app.setBusinessRegion(businessRegion.trim());
        app.setPhone(phone.trim());
        app.setOwnerFullName(ownerFullName.trim());
        app.setOwnerIdType(ownerIdType.trim());
        app.setOwnerIdNumber(ownerIdNumber.trim());
        app.setOwnerAddress(ownerAddress.trim());
        app.setBankName(bankName.trim());
        app.setBankAccountNumber(bankAccountNumber.trim());
        app.setBankAccountName(bankAccountName.trim());
        app.setMobileMoneyNumber(trimOrNull(mobileMoneyNumber));
        app.setProductsToSell(productsToSell.trim());
        app.setReason(reason.trim());

        if (nationalIdDoc != null && !nationalIdDoc.isEmpty()) {
            try { app.setNationalIdDocUrl(userService.saveKycDoc(nationalIdDoc)); }
            catch (Exception e) { return "redirect:/buyer/vendorapply?error=doc_upload"; }
        }
        if (businessRegDoc != null && !businessRegDoc.isEmpty()) {
            try { app.setBusinessRegDocUrl(userService.saveKycDoc(businessRegDoc)); }
            catch (Exception e) { return "redirect:/buyer/vendorapply?error=doc_upload"; }
        }

        vendorApplicationService.apply(user, app);
        return "redirect:/buyer/vendorapply?submitted";
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private String trimOrNull(String s) { return (s == null || s.trim().isEmpty()) ? null : s.trim(); }
}
