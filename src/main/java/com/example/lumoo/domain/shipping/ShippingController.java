package com.example.lumoo.domain.shipping;

import com.example.lumoo.shared.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ShippingController {

    @Autowired private ShippingService shippingService;

    @GetMapping("/shipping")
    public String calculatorPage(Model model) {
        model.addAttribute("coverageTypes", ShippingRate.Coverage.values());
        return "shipping";
    }

    @GetMapping("/api/shipping/rates")
    @ResponseBody
    public ResponseEntity<?> getRates(
            @RequestParam double weight,
            @RequestParam(defaultValue = "NATIONAL") String coverage) {
        if (weight <= 0 || weight > 1000) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid weight."));
        }
        ShippingRate.Coverage cov;
        try { cov = ShippingRate.Coverage.valueOf(coverage.toUpperCase()); }
        catch (IllegalArgumentException e) { cov = ShippingRate.Coverage.NATIONAL; }

        List<ShippingService.ShippingQuote> quotes = shippingService.getQuotes(weight, cov);
        return ResponseEntity.ok(ApiResponse.ok("Rates retrieved.", quotes));
    }
}
