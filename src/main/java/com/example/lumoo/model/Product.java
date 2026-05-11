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

    @Column(name = "approved")
    private boolean approved = false;

    @Column(name = "image_approved")
    private boolean imageApproved = false;

    @Column(name = "image_url")
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private User vendor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public boolean isImageApproved() { return imageApproved; }
    public void setImageApproved(boolean imageApproved) { this.imageApproved = imageApproved; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public User getVendor() { return vendor; }
    public void setVendor(User vendor) { this.vendor = vendor; }
}
