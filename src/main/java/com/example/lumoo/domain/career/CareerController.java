package com.example.lumoo.domain.career;
import com.example.lumoo.domain.admin.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/careers")
public class CareerController {
    @Autowired private CareerService careerService;
    @Autowired private SiteSettingsService siteSettingsService;
    @GetMapping({"", "/"})
    public String list(Model model) {
        model.addAttribute("postings", careerService.getActivePostings());
        model.addAttribute("siteSettings", siteSettingsService.get());
        return "careers";
    }
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        CareerPosting posting = careerService.findPostingById(id).orElse(null);
        if (posting == null || !posting.isActive()) return "redirect:/careers";
        model.addAttribute("posting", posting);
        model.addAttribute("siteSettings", siteSettingsService.get());
        return "careers-detail";
    }
    @PostMapping("/{id}/apply")
    public String apply(@PathVariable Long id,
                        @RequestParam String applicantName,
                        @RequestParam String email,
                        @RequestParam(required = false) String phone,
                        @RequestParam String coverLetter,
                        RedirectAttributes ra) {
        CareerPosting posting = careerService.findPostingById(id).orElse(null);
        if (posting == null || !posting.isActive()) return "redirect:/careers";
        careerService.submitApplication(id, applicantName, email, phone, coverLetter);
        ra.addFlashAttribute("applied", true);
        return "redirect:/careers/" + id + "?applied";
    }
}
