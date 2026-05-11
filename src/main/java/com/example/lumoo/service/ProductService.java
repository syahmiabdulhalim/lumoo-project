package com.example.lumoo.service;

import com.example.lumoo.model.Product;
import com.example.lumoo.model.User;
import com.example.lumoo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired private ProductRepository productRepository;

    public List<Product> getAll() {
        return productRepository.findAll();
    }

    public List<Product> searchByName(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }

    public List<Product> getByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> getApprovedByCategory(String category) {
        return productRepository.findByCategoryIgnoreCaseAndApproved(category);
    }

    public List<Product> getByVendor(User vendor) {
        return productRepository.findByVendor(vendor);
    }

    public List<Product> getPendingApproval() {
        return productRepository.findByApproved(false);
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public void addProduct(Product product, User vendor) {
        product.setVendor(vendor);
        product.setApproved(false);
        productRepository.save(product);
    }

    public boolean updateProduct(Long id, Product updated, User vendor) {
        Product existing = productRepository.findById(id).orElse(null);
        if (existing == null || !existing.getVendor().getId().equals(vendor.getId())) return false;
        updated.setId(id);
        updated.setVendor(vendor);
        productRepository.save(updated);
        return true;
    }

    public boolean deleteByVendor(Long id, User vendor) {
        Product existing = productRepository.findById(id).orElse(null);
        if (existing == null || !existing.getVendor().getId().equals(vendor.getId())) return false;
        productRepository.deleteById(id);
        return true;
    }

    public void approve(Long id) {
        productRepository.findById(id).ifPresent(p -> {
            p.setApproved(true);
            productRepository.save(p);
        });
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
