package com.example.lumoo.domain.product;

import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String categoryPage(@PathVariable String name,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        String categoryName = name.toLowerCase();
        Page<Product> productPage = productService.getApprovedByCategoryPaged(categoryName, PageRequest.of(page, 8));
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("categoryName", name);
        model.addAttribute("description", CATEGORY_DESCRIPTIONS.getOrDefault(categoryName,
                "Browse our selection of " + name + " products from verified vendors."));
        model.addAttribute("icon", CATEGORY_ICONS.getOrDefault(categoryName, "📦"));
        model.addAttribute("productCount", productPage.getTotalElements());
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
