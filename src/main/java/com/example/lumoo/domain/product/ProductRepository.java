package com.example.lumoo.domain.product;

import com.example.lumoo.domain.product.Product;
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
    // Tambah ini untuk penapisan kategori
    List<Product> findByCategory(String category);
    List<Product> findByApproved(boolean approved);
    List<Product> findByCategoryAndApproved(String category, boolean approved);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE LOWER(p.category) = LOWER(:category) AND p.approved = true")
    List<Product> findByCategoryIgnoreCaseAndApproved(@org.springframework.data.repository.query.Param("category") String category);

    // ── Public listing queries (approved + in-stock only) ────────────────────

    @org.springframework.data.jpa.repository.EntityGraph("Product.withVendor")
    @org.springframework.data.jpa.repository.Query(value = "SELECT p FROM Product p WHERE LOWER(p.category) = LOWER(:category) AND p.approved = true AND p.stock > 0", countQuery = "SELECT COUNT(p) FROM Product p WHERE LOWER(p.category) = LOWER(:category) AND p.approved = true AND p.stock > 0")
    Page<Product> findByCategoryIgnoreCaseAndApprovedPaged(@org.springframework.data.repository.query.Param("category") String category, Pageable pageable);

    List<Product> findByApprovedTrue();

    @org.springframework.data.jpa.repository.EntityGraph("Product.withVendor")
    @org.springframework.data.jpa.repository.Query(value = "SELECT p FROM Product p WHERE p.approved = true AND p.stock > 0", countQuery = "SELECT COUNT(p) FROM Product p WHERE p.approved = true AND p.stock > 0")
    Page<Product> findByApprovedTrue(Pageable pageable);

    // JOIN FETCH variants — vendor is LAZY, use these when vendor is rendered
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p LEFT JOIN FETCH p.vendor WHERE p.approved = true AND p.stock > 0")
    List<Product> findByApprovedTrueWithVendor();

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p LEFT JOIN FETCH p.vendor")
    List<Product> findAllWithVendor();

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p LEFT JOIN FETCH p.vendor WHERE LOWER(p.category) = LOWER(:category) AND p.approved = true AND p.stock > 0")
    List<Product> findByCategoryIgnoreCaseAndApprovedWithVendor(@org.springframework.data.repository.query.Param("category") String category);

    List<Product> findByApprovedTrueAndNameContainingIgnoreCase(String name);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p LEFT JOIN FETCH p.vendor WHERE p.approved = true AND p.stock > 0 AND LOWER(p.name) LIKE LOWER(CONCAT('%',:kw,'%'))")
    List<Product> findByApprovedTrueAndNameContainingIgnoreCaseWithVendor(@org.springframework.data.repository.query.Param("kw") String kw);

    @org.springframework.data.jpa.repository.EntityGraph("Product.withVendor")
    @org.springframework.data.jpa.repository.Query(value = "SELECT p FROM Product p WHERE p.approved = true AND p.stock > 0 AND LOWER(p.name) LIKE LOWER(CONCAT('%',:kw,'%'))", countQuery = "SELECT COUNT(p) FROM Product p WHERE p.approved = true AND p.stock > 0 AND LOWER(p.name) LIKE LOWER(CONCAT('%',:kw,'%'))")
    Page<Product> findByApprovedTrueAndNameContainingIgnoreCasePaged(@org.springframework.data.repository.query.Param("kw") String kw, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT * FROM products WHERE approved = 1 AND stock > 0 AND MATCH(name) AGAINST (:kw IN BOOLEAN MODE) LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}",
        countQuery = "SELECT COUNT(*) FROM products WHERE approved = 1 AND stock > 0 AND MATCH(name) AGAINST (:kw IN BOOLEAN MODE)",
        nativeQuery = true)
    Page<Product> fullTextSearch(@org.springframework.data.repository.query.Param("kw") String kw, Pageable pageable);

    List<Product> findByApprovedTrueAndImageApprovedFalse();

    List<Product> findByVendorAndApprovedTrue(User vendor);

    @org.springframework.data.jpa.repository.EntityGraph("Product.withVendor")
    Page<Product> findByVendorAndApprovedTrue(User vendor, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph("Product.withVendor")
    Page<Product> findByVendorAndApprovedTrueAndCategoryIgnoreCase(User vendor, String category, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.category FROM Product p WHERE p.vendor = :vendor AND p.approved = true ORDER BY p.category")
    List<String> findDistinctCategoriesByVendor(@org.springframework.data.repository.query.Param("vendor") User vendor);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT p.vendor FROM Product p WHERE p.approved = true AND p.vendor IS NOT NULL")
    List<User> findDistinctVendorsWithApprovedProducts();

    /** Atomic decrement — only succeeds if stock >= qty. Returns rows affected (1 = ok, 0 = insufficient). */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock - :qty WHERE p.id = :id AND p.stock >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    /** Return stock (on cancel / payment failure). */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock + :qty WHERE p.id = :id")
    void returnStock(@Param("id") Long id, @Param("qty") int qty);
}