package com.example.lumoo.domain.order;
import lombok.Getter;
import lombok.Setter;
import com.example.lumoo.domain.product.Product;
import jakarta.persistence.*;
@Entity 
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productName;
    private Integer quantity;
    private Double price;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
}
