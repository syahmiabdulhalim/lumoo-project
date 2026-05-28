package com.example.lumoo.domain.order;

import com.example.lumoo.domain.order.CartItem;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<CartItem, Long> {
    
    List<CartItem> findByUser(User user);
    
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    
    void deleteByUser(User user);
}