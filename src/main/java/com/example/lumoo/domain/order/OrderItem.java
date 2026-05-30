package com.example.lumoo.domain.order;
import com.example.lumoo.domain.product.Product;
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
    public Long getId() { return id; }
    public String getProductName() { return productName; }
    public Integer getQuantity() { return quantity; }
    public Double getPrice() { return price; }
    public Order getOrder() { return order; }
public Product getProduct() { return product; }
public void setProduct(Product product) { this.product = product; }
    public void setId(Long id) { this.id = id; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setPrice(Double price) { this.price = price; }
    public void setOrder(Order order) { this.order = order; }
}
