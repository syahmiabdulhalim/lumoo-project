package com.example.lumoo.domain.shipping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/riders")
public class AdminRiderController {

    @Autowired private RiderService riderService;

    @GetMapping({"", "/"})
    public String list(Model model) {
        model.addAttribute("riders", riderService.getAll());
        model.addAttribute("vehicleTypes", Rider.VehicleType.values());
        return "admin/riders";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute Rider rider, RedirectAttributes ra) {
        riderService.save(rider);
        ra.addFlashAttribute("flashMsg", "Rider added.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/riders";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute Rider form, RedirectAttributes ra) {
        riderService.findById(id).ifPresent(r -> {
            r.setName(form.getName());
            r.setPhone(form.getPhone());
            r.setVehicleType(form.getVehicleType());
            r.setArea(form.getArea());
            riderService.save(r);
        });
        ra.addFlashAttribute("flashMsg", "Rider updated.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/riders";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        riderService.toggle(id);
        ra.addFlashAttribute("flashMsg", "Rider status updated.");
        ra.addFlashAttribute("flashType", "blue");
        return "redirect:/admin/riders";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        riderService.delete(id);
        ra.addFlashAttribute("flashMsg", "Rider removed.");
        ra.addFlashAttribute("flashType", "red");
        return "redirect:/admin/riders";
    }
}
