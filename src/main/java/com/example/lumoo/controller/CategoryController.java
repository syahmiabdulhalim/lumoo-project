package com.example.lumoo.controller;

import com.example.lumoo.model.Product;
import com.example.lumoo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/category")
public class CategoryController {

    @Autowired private ProductService productService;

    private static final Map<String, String> CATEGORY_DESCRIPTIONS = Map.of(
        "roofing",    "Find various roofing materials including Decra, corrugated iron sheets, clay tiles, and more.",
        "cement",     "Explore a wide range of cement and concrete products for all your construction needs.",
        "building",   "Everything you need for building construction — bricks, blocks, sand, gravel, and structural materials.",
        "electrical", "Complete electrical supplies including wiring, cables, switches, panels, and lighting solutions.",
        "plumbing",   "All plumbing essentials — pipes, fittings, valves, water tanks, and sanitary ware.",
        "timber",     "Quality timber and wood products for framing, flooring, furniture, and finishing.",
        "paint",      "Premium paints and finishes for interior and exterior surfaces. Wide range of colors and brands.",
        "hardware",   "General hardware tools and accessories for professionals and DIY enthusiasts."
    );

    private static final Map<String, String> CATEGORY_ICONS = Map.of(
        "roofing", "🏠", "cement", "🧱", "building", "🏗️", "electrical", "⚡",
        "plumbing", "🔧", "timber", "🪵", "paint", "🎨", "hardware", "🔩"
    );

    @GetMapping("/{name}")
    public String categoryPage(@PathVariable String name, Model model) {
        String categoryName = name.toLowerCase();
        List<Product> products = productService.getApprovedByCategory(categoryName);
        model.addAttribute("products", products);
        model.addAttribute("categoryName", name);
        model.addAttribute("description", CATEGORY_DESCRIPTIONS.getOrDefault(categoryName,
                "Browse our selection of " + name + " products from verified vendors."));
        model.addAttribute("icon", CATEGORY_ICONS.getOrDefault(categoryName, "📦"));
        model.addAttribute("productCount", products.size());
        return "category";
    }

    @GetMapping
    public String allCategories(Model model) {
        model.addAttribute("categories", CATEGORY_DESCRIPTIONS.keySet());
        model.addAttribute("descriptions", CATEGORY_DESCRIPTIONS);
        model.addAttribute("icons", CATEGORY_ICONS);
        return "categories";
    }
}
