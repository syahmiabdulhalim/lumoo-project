package com.example.lumoo.domain.order;
import lombok.Getter;
import lombok.Setter;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.user.User;
import jakarta.persistence.*;
@Entity 
@Table(name = "cart_items", uniqueConstraints =
    @UniqueConstraint(name = "uk_cart_user_product", columnNames = {"user_id", "product_id"}))
@Getter
@Setter
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    private int quantity;
    private String name; 
    private double price;
}
