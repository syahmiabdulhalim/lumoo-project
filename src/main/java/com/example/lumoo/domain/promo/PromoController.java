package com.example.lumoo.domain.promo;

import com.example.lumoo.shared.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/promo")
public class PromoController {

    @Autowired private PromoService promoService;

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestParam String code,
                                      @RequestParam double orderTotal) {
        return promoService.validate(code, orderTotal)
                .map(p -> ResponseEntity.ok(ApiResponse.ok("Promo applied!", Map.of(
                        "code",           p.getCode(),
                        "discountType",   p.getDiscountType().name(),
                        "discountValue",  p.getDiscountValue(),
                        "discountAmount", p.discountAmount(orderTotal),
                        "finalTotal",     p.apply(orderTotal)
                ))))
                .orElse(ResponseEntity.badRequest().body(
                        ApiResponse.error("Invalid or expired promo code.")));
    }
}
