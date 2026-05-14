package com.example.lumoo.model;

import jakarta.persistence.*;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user", columnList = "user_id")
})
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String customerName;
    private String address;
    private String paymentMethod;
    private String trackingNumber;
    private String returnReason;
    private String paymentProofUrl;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
private LocalDateTime createdAt;
    private LocalDateTime orderDate;
    private String status;
    private double totalAmount;
    private double adminCommission;
    private double vendorEarnings;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<OrderItem> items;
@PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    // --- GETTERS & SETTERS (BETUL) ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }

    public String getPaymentProofUrl() { return paymentProofUrl; }
    public void setPaymentProofUrl(String paymentProofUrl) { this.paymentProofUrl = paymentProofUrl; }

    public User getUser() { return user; } // PULANGKAN 'User', BUKAN 'Object'
    public void setUser(User user) { this.user = user; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public double getAdminCommission() { return adminCommission; }
    public void setAdminCommission(double adminCommission) { this.adminCommission = adminCommission; }

    public double getVendorEarnings() { return vendorEarnings; }
    public void setVendorEarnings(double vendorEarnings) { this.vendorEarnings = vendorEarnings; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}