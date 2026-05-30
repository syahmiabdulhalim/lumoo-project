package com.example.lumoo.domain.inquiry;
import com.example.lumoo.domain.inquiry.Inquiry;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.inquiry.InquiryRepository;
import com.example.lumoo.domain.product.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
@Service
public class InquiryService {
    @Autowired private InquiryRepository inquiryRepository;
    @Autowired private ProductRepository productRepository;
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
    }
    public List<Inquiry> getAll() {
        return inquiryRepository.findAllByOrderByCreatedAtDesc();
    }
    public void delete(Long id) {
        inquiryRepository.deleteById(id);
    }
}
