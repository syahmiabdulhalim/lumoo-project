package com.example.lumoo.domain.product;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.Review;
import com.example.lumoo.domain.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Review r JOIN FETCH r.user WHERE r.product.id = :productId ORDER BY r.createdAt DESC")
    List<Review> findByProductId(@org.springframework.data.repository.query.Param("productId") Long productId);
    boolean existsByUserAndProduct(User user, Product product);
}
