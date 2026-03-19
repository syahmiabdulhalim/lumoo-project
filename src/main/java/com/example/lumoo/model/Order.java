package com.example.lumoo.model;

import jakarta.persistence.*;
import java.util.List;
import java.time.LocalDateTime;

@Entity @Table(name = "orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customerName;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime orderDate;

    private String status;
    private double totalAmount;
    private double adminCommission;
    private double vendorEarnings;

    @OneToMany(cascade = CascadeType.ALL)
    private List<OrderItem> items;

    // Getters & Setters
    public void setUser(User user) { this.user = user; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public Object getUser() {
        throw new UnsupportedOperationException("Unimplemented method 'getUser'");
    }
    public double getAdminCommission() {
        return adminCommission;
    }

    public void setAdminCommission(double adminCommission) {
        this.adminCommission = adminCommission;
    }

    public double getVendorEarnings() {
        return vendorEarnings;
    }

    public void setVendorEarnings(double vendorEarnings) {
        this.vendorEarnings = vendorEarnings;
    }
}