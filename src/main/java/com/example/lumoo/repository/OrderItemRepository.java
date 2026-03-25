package com.example.lumoo.repository;

import com.example.lumoo.model.OrderItem;
import com.example.lumoo.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Mencari semua item yang terkandung dalam satu order id yang spesifik
    List<OrderItem> findByOrder(Order order);
}