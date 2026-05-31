package com.example.lumoo.domain.about;

import com.example.lumoo.domain.admin.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/about")
public class AboutController {

    @Autowired private AboutService aboutService;
    @Autowired private SiteSettingsService siteSettingsService;

    @GetMapping({"", "/"})
    public String page(Model model) {
        model.addAttribute("content", aboutService.getContent());
        model.addAttribute("members", aboutService.getActiveMembers());
        model.addAttribute("values", aboutService.getAllValues());
        model.addAttribute("siteSettings", siteSettingsService.get());
        return "about";
    }
}
