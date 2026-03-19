package com.example.lumoo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity 
@Table(name = "inquiries")
public class Inquiry {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String subject;
    private String buyerName; 

    @Column(columnDefinition = "TEXT")
    private String message;

    // Selaraskan kepada createdAt supaya sepadan dengan Controller anda
    private LocalDateTime createdAt = LocalDateTime.now();

    // Pastikan medan Product ini wujud untuk hubungan database
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // --- CONSTRUCTORS ---
    public Inquiry() {}

    public Inquiry(String name, String email, String subject, String message) {
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    // --- GETTERS ---
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public String getBuyerName() { return buyerName; }
    public Product getProduct() { return product; } // Mesti pulangkan Product
    public LocalDateTime getCreatedAt() { return createdAt; }

    // --- SETTERS ---
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setMessage(String message) { this.message = message; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public void setProduct(Product product) { this.product = product; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}