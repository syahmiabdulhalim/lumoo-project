package com.example.lumoo.domain.promo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/promos")
public class AdminPromoController {

    @Autowired private PromoService promoService;

    @GetMapping({"", "/"})
    public String list(Model model) {
        model.addAttribute("promos", promoService.getAll());
        model.addAttribute("newPromo", new PromoCode());
        model.addAttribute("discountTypes", PromoCode.DiscountType.values());
        return "admin/promos";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute PromoCode promo, RedirectAttributes ra) {
        promoService.save(promo);
        ra.addFlashAttribute("flashMsg", "Promo code created.");
        ra.addFlashAttribute("flashType", "green");
        return "redirect:/admin/promos";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        promoService.toggle(id);
        ra.addFlashAttribute("flashMsg", "Promo code updated.");
        ra.addFlashAttribute("flashType", "blue");
        return "redirect:/admin/promos";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        promoService.delete(id);
        ra.addFlashAttribute("flashMsg", "Promo code deleted.");
        ra.addFlashAttribute("flashType", "red");
        return "redirect:/admin/promos";
    }
}
