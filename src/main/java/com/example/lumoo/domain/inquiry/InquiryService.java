package com.example.lumoo.domain.inquiry;
import com.example.lumoo.domain.inquiry.Inquiry;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.inquiry.InquiryRepository;
import com.example.lumoo.domain.product.ProductRepository;
import com.example.lumoo.infrastructure.email.EmailService;
import com.example.lumoo.infrastructure.email.EmailTemplates;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
@Service
public class InquiryService {
    private static final Logger log = LoggerFactory.getLogger(InquiryService.class);

    @Autowired private InquiryRepository inquiryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private EmailService emailService;
    public void send(Long productId, String buyerName, String email, String subject, String message) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return;
        Inquiry inquiry = new Inquiry();
        inquiry.setBuyerName(buyerName);
        inquiry.setEmail(email);
        inquiry.setSubject(subject != null && !subject.isBlank() ? subject : "Inquiry about " + product.getName());
        inquiry.setMessage(message);
        inquiry.setProduct(product);
        inquiry.setCreatedAt(LocalDateTime.now());
        inquiryRepository.save(inquiry);
        if (product.getVendor() != null && product.getVendor().getEmail() != null) {
            String vendorName = product.getVendor().getUsername();
            emailService.sendEmail(product.getVendor().getEmail(),
                    "New inquiry: " + inquiry.getSubject(),
                    EmailTemplates.inquiryReceived(vendorName, buyerName,
                            product.getName(), inquiry.getSubject(), message, email));
        }
    }
    public List<Inquiry> getAll() {
        return inquiryRepository.findAllByOrderByCreatedAtDesc();
    }
    public Page<Inquiry> getPage(int page, int size) {
        return inquiryRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }
    public void delete(Long id) {
        inquiryRepository.deleteById(id);
    }
}
