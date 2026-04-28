package com.example.lumoo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String category;
    private Double price;
    private Boolean approved = false;
    @Column(name = "approved")

public boolean isApproved() { return approved; }
public void setApproved(boolean approved) { this.approved = approved; }
    
    @Column(name = "image_url")
    private String imageUrl;

    // PEMBETULAN: Hubungkan produk dengan User (Vendor)
    @ManyToOne
    @JoinColumn(name = "vendor_id") // Ini akan jadi 'foreign key' dalam table products
    private User vendor;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    // Tambah Getter & Setter untuk Vendor
    public User getVendor() { return vendor; }
    public void setVendor(User vendor) { this.vendor = vendor; }
}