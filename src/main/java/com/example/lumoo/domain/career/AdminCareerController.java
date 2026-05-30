package com.example.lumoo.domain.career;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller
@RequestMapping("/admin/careers")
public class AdminCareerController {
    @Autowired private CareerService careerService;
    @GetMapping({"", "/"})
    public String list(Model model) {
        var postings = careerService.getAllPostings();
        model.addAttribute("postings", postings);
        model.addAttribute("newApplicationCount", careerService.countNewApplications());
        for (CareerPosting p : postings) {
            model.addAttribute("appCount_" + p.getId(), careerService.countApplicationsForPosting(p));
        }
        return "admin/careers/list";
    }
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("posting", new CareerPosting());
        model.addAttribute("editing", false);
        model.addAttribute("jobTypes", CareerPosting.JobType.values());
        return "admin/careers/form";
    }
    @PostMapping("/new")
    public String create(@ModelAttribute CareerPosting posting, RedirectAttributes ra) {
        careerService.savePosting(posting);
        ra.addFlashAttribute("flashMsg", "Job posting created.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/careers";
    }
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        CareerPosting posting = careerService.findPostingById(id).orElse(null);
        if (posting == null) return "redirect:/admin/careers";
        model.addAttribute("posting", posting);
        model.addAttribute("editing", true);
        model.addAttribute("jobTypes", CareerPosting.JobType.values());
        return "admin/careers/form";
    }
    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, @ModelAttribute CareerPosting updated, RedirectAttributes ra) {
        CareerPosting existing = careerService.findPostingById(id).orElse(null);
        if (existing == null) return "redirect:/admin/careers";
        existing.setTitle(updated.getTitle());
        existing.setDepartment(updated.getDepartment());
        existing.setLocation(updated.getLocation());
        existing.setType(updated.getType());
        existing.setDescription(updated.getDescription());
        existing.setRequirements(updated.getRequirements());
        existing.setActive(updated.isActive());
        careerService.savePosting(existing);
        ra.addFlashAttribute("flashMsg", "Job posting updated.");
        ra.addFlashAttribute("flashType", "blue");
        return "redirect:/admin/careers";
    }
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        careerService.deletePosting(id);
        ra.addFlashAttribute("flashMsg", "Job posting deleted.");
        ra.addFlashAttribute("flashType", "red");
        return "redirect:/admin/careers";
    }
    @GetMapping("/{id}/applications")
    public String applications(@PathVariable Long id, Model model) {
        CareerPosting posting = careerService.findPostingById(id).orElse(null);
        if (posting == null) return "redirect:/admin/careers";
        model.addAttribute("posting", posting);
        model.addAttribute("applications", careerService.getApplicationsForPosting(posting));
        model.addAttribute("statuses", CareerApplication.Status.values());
        return "admin/careers/applications";
    }
    @PostMapping("/applications/{appId}/status")
    public String updateStatus(@PathVariable Long appId,
                               @RequestParam CareerApplication.Status status,
                               @RequestParam Long postingId,
                               RedirectAttributes ra) {
        careerService.updateApplicationStatus(appId, status);
        ra.addFlashAttribute("flashMsg", "Status updated.");
        ra.addFlashAttribute("flashType", "blue");
        return "redirect:/admin/careers/" + postingId + "/applications";
    }
}
