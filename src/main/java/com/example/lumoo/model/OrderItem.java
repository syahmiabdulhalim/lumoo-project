package com.example.lumoo.model;

import jakarta.persistence.*;

@Entity 
@Table(name = "order_items")
public class OrderItem {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;
    private Integer quantity;
    private Double price;
@ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    // --- GETTERS ---
    public Long getId() { return id; }
    public String getProductName() { return productName; }
    public Integer getQuantity() { return quantity; }
    public Double getPrice() { return price; }
    public Order getOrder() { return order; }

    // --- SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setPrice(Double price) { this.price = price; }
    public void setOrder(Order order) { this.order = order; }
}