package com.example.lumoo.domain.inquiry;
import com.example.lumoo.domain.inquiry.InquiryService;
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
                              @RequestParam(required = false) String subject,
                              @RequestParam String message) {
        inquiryService.send(productId, buyerName, email, subject, message);
        return "redirect:/product/" + productId + "?inquiry_sent";
    }
}
