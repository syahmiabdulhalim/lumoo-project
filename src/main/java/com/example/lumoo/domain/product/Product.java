package com.example.lumoo.domain.product;
import lombok.Getter;
import lombok.Setter;
import com.example.lumoo.domain.user.User;
import jakarta.persistence.*;
@NamedEntityGraph(name = "Product.withVendor", attributeNodes = @NamedAttributeNode("vendor"))
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_approved", columnList = "approved"),
    @Index(name = "idx_product_vendor", columnList = "vendor_id"),
    @Index(name = "idx_product_category_approved", columnList = "category,approved")
})
@Getter
@Setter
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
    private Double weightKg;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private User vendor;
    public boolean isApproved() { return approved != null && approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public boolean isImageApproved() { return imageApproved != null && imageApproved; }
    public void setImageApproved(boolean imageApproved) { this.imageApproved = imageApproved; }
}
