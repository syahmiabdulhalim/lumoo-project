package com.example.lumoo.controller;

import com.example.lumoo.service.InquiryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/inquiry")
public class InquiryController {

    @Autowired private InquiryService inquiryService;

    @PostMapping("/send/{productId}")
    public String sendInquiry(@PathVariable Long productId,
                              @RequestParam String buyerName,
                              @RequestParam String email,
                              @RequestParam String message) {
        inquiryService.send(productId, buyerName, email, message);
        return "redirect:/?inquiry_sent";
    }
}
