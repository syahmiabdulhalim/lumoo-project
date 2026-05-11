package com.example.lumoo.repository;

import com.example.lumoo.model.OrderItem;
import com.example.lumoo.model.Order;
import com.example.lumoo.model.Product;
import com.example.lumoo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder(Order order);
    boolean existsByOrderUserAndOrderStatusAndProduct(User user, String status, Product product);
}