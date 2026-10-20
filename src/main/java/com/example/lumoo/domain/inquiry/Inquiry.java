package com.example.lumoo.domain.inquiry;
import lombok.Getter;
import lombok.Setter;
import com.example.lumoo.domain.product.Product;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity 
@Table(name = "inquiries")
@Getter
@Setter
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
    private LocalDateTime createdAt = LocalDateTime.now();
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    public Inquiry() {}
    public Inquiry(String name, String email, String subject, String message) {
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }
}
