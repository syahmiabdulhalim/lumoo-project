package com.example.lumoo.repository;

import com.example.lumoo.model.Order;
import com.example.lumoo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByOrderDateDesc(User user);
    @Query("SELECT o FROM Order o JOIN o.items i WHERE i.product.vendor.id = :vendorId")
List<Order> findOrdersByVendorId(@Param("vendorId") Long vendorId);
}