package com.example.lumoo.domain.product;
import com.example.lumoo.domain.user.User;
import jakarta.persistence.*;
@NamedEntityGraph(name = "Product.withVendor", attributeNodes = @NamedAttributeNode("vendor"))
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_approved", columnList = "approved"),
    @Index(name = "idx_product_vendor", columnList = "vendor_id"),
    @Index(name = "idx_product_category_approved", columnList = "category,approved")
})
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String category;
    private Double price;
    @Column(nullable = false, columnDefinition = "int default 0")
    private int stock;
    @Column(name = "approved")
    private Boolean approved = false;
    @Column(name = "image_approved")
    private Boolean imageApproved = false;
    @Column(name = "image_url")
    private String imageUrl;
    @Column(columnDefinition = "TEXT")
    private String description;
    @ManyToOne(fetch = FetchType.LAZY)
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
    public boolean isApproved() { return approved != null && approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public boolean isImageApproved() { return imageApproved != null && imageApproved; }
    public void setImageApproved(boolean imageApproved) { this.imageApproved = imageApproved; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getVendor() { return vendor; }
    public void setVendor(User vendor) { this.vendor = vendor; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
}
