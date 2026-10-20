package com.example.lumoo.domain.product;
import lombok.Getter;
import lombok.Setter;
import com.example.lumoo.domain.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name = "reviews", uniqueConstraints =
    @UniqueConstraint(name = "uk_review_user_product", columnNames = {"user_id", "product_id"}))
@Getter
@Setter
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String reviewerName;
    private int rating; 
    @Column(columnDefinition = "TEXT")
    private String comment; 
    private LocalDateTime createdAt;
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
