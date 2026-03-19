package com.example.lumoo.repository;

import com.example.lumoo.model.Order;
import com.example.lumoo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByOrderDateDesc(User user);
}