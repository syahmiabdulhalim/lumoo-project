package com.example.lumoo.repository;

import com.example.lumoo.model.CartItem;
import com.example.lumoo.model.User;
import com.example.lumoo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<CartItem, Long> {
    
    // Cari semua barang dalam bakul milik seorang user sahaja
    List<CartItem> findByUser(User user);
    
    // Cari jika produk yang sama sudah ada dalam bakul user (supaya kita cuma update quantity)
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    
    // Padam semua barang dalam bakul selepas user berjaya 'Checkout'
    void deleteByUser(User user);
}