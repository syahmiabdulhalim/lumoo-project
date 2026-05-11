package com.example.lumoo.service;

import com.example.lumoo.model.Product;
import com.example.lumoo.model.User;
import com.example.lumoo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {

    @Autowired private ProductRepository productRepository;

    @Value("${app.upload.dir:/app/uploads/products}")
    private String uploadDir;

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

    public List<Product> getPendingImageApproval() {
        return productRepository.findByApprovedTrueAndImageApprovedFalse();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public String saveImage(MultipartFile file) throws IOException {
        String extension = "";
        String original = file.getOriginalFilename();
        if (original != null && original.contains(".")) {
            extension = original.substring(original.lastIndexOf("."));
        }
        String filename = UUID.randomUUID() + extension;
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/products/" + filename;
    }

    public void addProduct(Product product, User vendor) {
        product.setVendor(vendor);
        product.setApproved(false);
        product.setImageApproved(false);
        productRepository.save(product);
    }

    public boolean updateProduct(Long id, Product updated, User vendor, MultipartFile image) throws IOException {
        Product existing = productRepository.findById(id).orElse(null);
        if (existing == null || !existing.getVendor().getId().equals(vendor.getId())) return false;

        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setPrice(updated.getPrice());

        boolean imageChanged = false;
        if (image != null && !image.isEmpty()) {
            existing.setImageUrl(saveImage(image));
            imageChanged = true;
        } else if (updated.getImageUrl() != null && !updated.getImageUrl().isBlank()
                && !updated.getImageUrl().equals(existing.getImageUrl())) {
            existing.setImageUrl(updated.getImageUrl());
            imageChanged = true;
        }

        if (imageChanged) existing.setImageApproved(false);
        productRepository.save(existing);
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
            p.setImageApproved(true);
            productRepository.save(p);
        });
    }

    public void approveImage(Long id) {
        productRepository.findById(id).ifPresent(p -> {
            p.setImageApproved(true);
            productRepository.save(p);
        });
    }

    public void rejectImage(Long id) {
        productRepository.findById(id).ifPresent(p -> {
            p.setImageUrl(null);
            p.setImageApproved(false);
            productRepository.save(p);
        });
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
