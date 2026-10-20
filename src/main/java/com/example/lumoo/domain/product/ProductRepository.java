package com.example.lumoo.domain.product;
import org.springframework.data.jpa.repository.EntityGraph;
import com.example.lumoo.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByVendor(User vendor);
    List<Product> findByCategory(String category);
    List<Product> findByApproved(boolean approved);
    Page<Product> findByApproved(boolean approved, Pageable pageable);
    long countByApproved(boolean approved);
    Page<Product> findByApprovedTrueAndImageApprovedFalse(Pageable pageable);
    long countByApprovedTrueAndImageApprovedFalse();
    @EntityGraph("Product.withVendor")
    @Query(value = "SELECT p FROM Product p WHERE LOWER(p.category) = LOWER(:category) AND p.approved = true AND p.stock > 0", countQuery = "SELECT COUNT(p) FROM Product p WHERE LOWER(p.category) = LOWER(:category) AND p.approved = true AND p.stock > 0")
    Page<Product> findByCategoryIgnoreCaseAndApprovedPaged(@Param("category") String category, Pageable pageable);
    List<Product> findByApprovedTrue();
    @EntityGraph("Product.withVendor")
    @Query(value = "SELECT p FROM Product p WHERE p.approved = true AND p.stock > 0", countQuery = "SELECT COUNT(p) FROM Product p WHERE p.approved = true AND p.stock > 0")
    Page<Product> findByApprovedTrue(Pageable pageable);
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.vendor WHERE p.approved = true AND p.stock > 0")
    List<Product> findByApprovedTrueWithVendor();
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.vendor")
    List<Product> findAllWithVendor();
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.vendor WHERE LOWER(p.category) = LOWER(:category) AND p.approved = true AND p.stock > 0")
    List<Product> findByCategoryIgnoreCaseAndApprovedWithVendor(@Param("category") String category);
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.vendor WHERE p.approved = true AND p.stock > 0 AND LOWER(p.name) LIKE LOWER(CONCAT('%',:kw,'%'))")
    List<Product> findByApprovedTrueAndNameContainingIgnoreCaseWithVendor(@Param("kw") String kw);
    @EntityGraph("Product.withVendor")
    @Query(value = "SELECT p FROM Product p WHERE p.approved = true AND p.stock > 0 AND LOWER(p.name) LIKE LOWER(CONCAT('%',:kw,'%'))", countQuery = "SELECT COUNT(p) FROM Product p WHERE p.approved = true AND p.stock > 0 AND LOWER(p.name) LIKE LOWER(CONCAT('%',:kw,'%'))")
    Page<Product> findByApprovedTrueAndNameContainingIgnoreCasePaged(@Param("kw") String kw, Pageable pageable);
    @Query(
        value = "SELECT * FROM products WHERE approved = 1 AND stock > 0 AND MATCH(name) AGAINST (:kw IN BOOLEAN MODE) LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}",
        countQuery = "SELECT COUNT(*) FROM products WHERE approved = 1 AND stock > 0 AND MATCH(name) AGAINST (:kw IN BOOLEAN MODE)",
        nativeQuery = true)
    Page<Product> fullTextSearch(@Param("kw") String kw, Pageable pageable);
    List<Product> findByApprovedTrueAndImageApprovedFalse();
    List<Product> findByVendorAndApprovedTrue(User vendor);
    @EntityGraph("Product.withVendor")
    Page<Product> findByVendorAndApprovedTrue(User vendor, Pageable pageable);
    @EntityGraph("Product.withVendor")
    Page<Product> findByVendorAndApprovedTrueAndCategoryIgnoreCase(User vendor, String category, Pageable pageable);
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.vendor = :vendor AND p.approved = true ORDER BY p.category")
    List<String> findDistinctCategoriesByVendor(@Param("vendor") User vendor);
    @Query("SELECT DISTINCT p.vendor FROM Product p WHERE p.approved = true AND p.vendor IS NOT NULL")
    List<User> findDistinctVendorsWithApprovedProducts();
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock - :qty WHERE p.id = :id AND p.stock >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock + :qty WHERE p.id = :id")
    void returnStock(@Param("id") Long id, @Param("qty") int qty);
}
