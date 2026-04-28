package com.example.lumoo.repository;

import com.example.lumoo.model.Product;
import com.example.lumoo.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByVendor(User vendor);
    // Tambah ini untuk penapisan kategori
    List<Product> findByCategory(String category);
    List<Product> findByApproved(boolean approved);
    List<Product> findByCategoryAndApproved(String category, boolean approved);
}