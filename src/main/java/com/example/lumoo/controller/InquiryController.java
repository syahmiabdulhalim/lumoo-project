package com.example.lumoo.controller;

import com.example.lumoo.model.Inquiry;
import com.example.lumoo.model.Product;
import com.example.lumoo.repository.InquiryRepository;
import com.example.lumoo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/inquiry")
public class InquiryController {
    
    @Autowired private InquiryRepository inquiryRepository;
    @Autowired private ProductRepository productRepository;

    @PostMapping("/send/{productId}")
    public String sendInquiry(@PathVariable Long productId, 
                             @RequestParam String buyerName,
                             @RequestParam String email,
                             @RequestParam String message) {
        
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            Inquiry inquiry = new Inquiry();
            inquiry.setBuyerName(buyerName);
            inquiry.setEmail(email);
            inquiry.setMessage(message);
            inquiry.setProduct(product);
            inquiry.setCreatedAt(java.time.LocalDateTime.now());
            
            inquiryRepository.save(inquiry);
        }
        return "redirect:/?inquiry_sent";
    }
}