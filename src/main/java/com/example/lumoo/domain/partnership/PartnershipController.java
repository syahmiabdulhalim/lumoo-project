package com.example.lumoo.domain.partnership;
import com.example.lumoo.domain.admin.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/partnerships")
public class PartnershipController {
    @Autowired private PartnershipService partnershipService;
    @Autowired private SiteSettingsService siteSettingsService;
    @GetMapping({"", "/"})
    public String page(Model model) {
        model.addAttribute("partnerTypes", PartnershipApplication.PartnerType.values());
        model.addAttribute("siteSettings", siteSettingsService.get());
        return "partnerships";
    }
    @PostMapping("/apply")
    public String apply(@RequestParam String companyName,
                        @RequestParam String contactName,
                        @RequestParam String email,
                        @RequestParam(required = false) String phone,
                        @RequestParam PartnershipApplication.PartnerType partnerType,
                        @RequestParam String message,
                        RedirectAttributes ra) {
        partnershipService.submit(companyName, contactName, email, phone, partnerType, message);
        ra.addFlashAttribute("submitted", true);
        return "redirect:/partnerships";
    }
}
