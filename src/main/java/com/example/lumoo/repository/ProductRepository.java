package com.example.lumoo.repository;

import com.example.lumoo.model.Product;
import com.example.lumoo.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByVendor(User vendor);
    // Tambah ini untuk penapisan kategori
    List<Product> findByCategory(String category);
    List<Product> findByApproved(boolean approved);
    List<Product> findByCategoryAndApproved(String category, boolean approved);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE LOWER(p.category) = LOWER(:category) AND p.approved = true")
    List<Product> findByCategoryIgnoreCaseAndApproved(@org.springframework.data.repository.query.Param("category") String category);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE LOWER(p.category) = LOWER(:category) AND p.approved = true")
    Page<Product> findByCategoryIgnoreCaseAndApprovedPaged(@org.springframework.data.repository.query.Param("category") String category, Pageable pageable);

    List<Product> findByApprovedTrueAndImageApprovedFalse();

    List<Product> findByVendorAndApprovedTrue(User vendor);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.vendor FROM Product p WHERE p.approved = true AND p.vendor IS NOT NULL")
    List<User> findDistinctVendorsWithApprovedProducts();
}