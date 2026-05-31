package com.example.lumoo.domain.shipping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/shipping")
public class AdminShippingController {

    @Autowired private ShippingService shippingService;

    @GetMapping({"", "/"})
    public String list(Model model) {
        model.addAttribute("rates", shippingService.getAll());
        model.addAttribute("coverageTypes", ShippingRate.Coverage.values());
        return "admin/shipping";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute ShippingRate rate, RedirectAttributes ra) {
        shippingService.save(rate);
        ra.addFlashAttribute("flashMsg", "Courier rate added.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/shipping";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @ModelAttribute ShippingRate form, RedirectAttributes ra) {
        shippingService.findById(id).ifPresent(r -> {
            r.setCourierName(form.getCourierName());
            r.setCourierEmoji(form.getCourierEmoji());
            r.setCoverage(form.getCoverage());
            r.setMinWeightKg(form.getMinWeightKg());
            r.setMaxWeightKg(form.getMaxWeightKg());
            r.setBaseRateGmd(form.getBaseRateGmd());
            r.setPerKgRateGmd(form.getPerKgRateGmd());
            r.setEstimatedDaysMin(form.getEstimatedDaysMin());
            r.setEstimatedDaysMax(form.getEstimatedDaysMax());
            r.setTrackingUrlTemplate(form.getTrackingUrlTemplate());
            r.setDescription(form.getDescription());
            r.setDisplayOrder(form.getDisplayOrder());
            shippingService.save(r);
        });
        ra.addFlashAttribute("flashMsg", "Rate updated.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/shipping";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        shippingService.toggle(id);
        ra.addFlashAttribute("flashMsg", "Rate toggled.");
        ra.addFlashAttribute("flashType", "blue");
        return "redirect:/admin/shipping";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        shippingService.delete(id);
        ra.addFlashAttribute("flashMsg", "Rate deleted.");
        ra.addFlashAttribute("flashType", "red");
        return "redirect:/admin/shipping";
    }
}
