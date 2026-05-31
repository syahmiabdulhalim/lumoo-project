package com.example.lumoo.domain.about;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/about")
public class AdminAboutController {

    @Autowired private AboutService aboutService;

    @GetMapping({"", "/"})
    public String editor(Model model) {
        model.addAttribute("content", aboutService.getContent());
        model.addAttribute("members", aboutService.getAllMembers());
        model.addAttribute("values", aboutService.getAllValues());
        model.addAttribute("newMember", new TeamMember());
        model.addAttribute("newValue", new CompanyValue());
        return "admin/about/editor";
    }

    @PostMapping("/content")
    public String saveContent(@ModelAttribute AboutContent content, RedirectAttributes ra) {
        aboutService.saveContent(content);
        ra.addFlashAttribute("flashMsg", "About page updated.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/about";
    }

    @PostMapping("/team/new")
    public String addMember(@ModelAttribute TeamMember member, RedirectAttributes ra) {
        aboutService.saveMember(member);
        ra.addFlashAttribute("flashMsg", "Team member added.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/about#team";
    }

    @PostMapping("/team/edit/{id}")
    public String updateMember(@PathVariable Long id, @ModelAttribute TeamMember updated, RedirectAttributes ra) {
        aboutService.findMemberById(id).ifPresent(m -> {
            m.setName(updated.getName());
            m.setRole(updated.getRole());
            m.setBio(updated.getBio());
            m.setImageUrl(updated.getImageUrl());
            m.setDisplayOrder(updated.getDisplayOrder());
            m.setActive(updated.isActive());
            aboutService.saveMember(m);
        });
        ra.addFlashAttribute("flashMsg", "Team member updated.");
        ra.addFlashAttribute("flashType", "blue");
        return "redirect:/admin/about#team";
    }

    @PostMapping("/team/delete/{id}")
    public String deleteMember(@PathVariable Long id, RedirectAttributes ra) {
        aboutService.deleteMember(id);
        ra.addFlashAttribute("flashMsg", "Team member removed.");
        ra.addFlashAttribute("flashType", "red");
        return "redirect:/admin/about#team";
    }

    @PostMapping("/values/new")
    public String addValue(@ModelAttribute CompanyValue value, RedirectAttributes ra) {
        aboutService.saveValue(value);
        ra.addFlashAttribute("flashMsg", "Value added.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/about#values";
    }

    @PostMapping("/values/edit/{id}")
    public String updateValue(@PathVariable Long id, @ModelAttribute CompanyValue updated, RedirectAttributes ra) {
        aboutService.findValueById(id).ifPresent(v -> {
            v.setIcon(updated.getIcon());
            v.setTitle(updated.getTitle());
            v.setDescription(updated.getDescription());
            v.setDisplayOrder(updated.getDisplayOrder());
            aboutService.saveValue(v);
        });
        ra.addFlashAttribute("flashMsg", "Value updated.");
        ra.addFlashAttribute("flashType", "blue");
        return "redirect:/admin/about#values";
    }

    @PostMapping("/values/delete/{id}")
    public String deleteValue(@PathVariable Long id, RedirectAttributes ra) {
        aboutService.deleteValue(id);
        ra.addFlashAttribute("flashMsg", "Value removed.");
        ra.addFlashAttribute("flashType", "red");
        return "redirect:/admin/about#values";
    }
}
