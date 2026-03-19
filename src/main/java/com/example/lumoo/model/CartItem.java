package com.example.lumoo.model;

public class CartItem {
    private Long productId;
    private String name;
    private Double price;
    private Integer quantity;

    public CartItem(Long productId, String name, Double price, Integer quantity) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    // Getters & Setters
    public Long getProductId() { return productId; }
    public String getName() { return name; }
    public Double getPrice() { return price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}