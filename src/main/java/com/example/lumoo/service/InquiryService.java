package com.example.lumoo.service;

import com.example.lumoo.model.Inquiry;
import com.example.lumoo.model.Product;
import com.example.lumoo.repository.InquiryRepository;
import com.example.lumoo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InquiryService {

    @Autowired private InquiryRepository inquiryRepository;
    @Autowired private ProductRepository productRepository;

    public void send(Long productId, String buyerName, String email, String message) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return;
        Inquiry inquiry = new Inquiry();
        inquiry.setBuyerName(buyerName);
        inquiry.setEmail(email);
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
