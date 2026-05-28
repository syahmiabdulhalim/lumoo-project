package com.example.lumoo.domain.order;

import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByOrderDateDesc(User user);
    List<Order> findByCreatedAtBeforeAndCustomerNameNot(LocalDateTime before, String customerName);
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId")
    List<Order> findOrdersByVendorId(@Param("vendorId") Long vendorId);
}