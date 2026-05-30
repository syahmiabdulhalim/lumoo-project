package com.example.lumoo.domain.partnership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin/partnerships")
public class AdminPartnershipController {
    @Autowired private PartnershipService partnershipService;
    @GetMapping({"", "/"})
    public String list(Model model) {
        model.addAttribute("applications", partnershipService.getAll());
        model.addAttribute("statuses", PartnershipApplication.Status.values());
        return "admin/partnerships/list";
    }
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        PartnershipApplication app = partnershipService.findById(id).orElse(null);
        if (app == null) return "redirect:/admin/partnerships";
        model.addAttribute("application", app);
        model.addAttribute("statuses", PartnershipApplication.Status.values());
        return "admin/partnerships/detail";
    }
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam PartnershipApplication.Status status,
                               RedirectAttributes ra) {
        partnershipService.updateStatus(id, status);
        ra.addFlashAttribute("flashMsg", "Status updated.");
        ra.addFlashAttribute("flashType", "blue");
        return "redirect:/admin/partnerships/" + id;
    }
}
