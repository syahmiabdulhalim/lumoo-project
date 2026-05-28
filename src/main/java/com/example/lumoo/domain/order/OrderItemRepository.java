package com.example.lumoo.domain.order;

import com.example.lumoo.domain.order.OrderItem;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder(Order order);
    boolean existsByOrderUserAndOrderStatusAndProduct(User user, String status, Product product);
}