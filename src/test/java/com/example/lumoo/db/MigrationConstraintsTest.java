package com.example.lumoo.db;
import com.example.lumoo.domain.order.CartItem;
import com.example.lumoo.domain.order.CartRepository;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.ProductRepository;
import com.example.lumoo.domain.product.Review;
import com.example.lumoo.domain.product.ReviewRepository;
import com.example.lumoo.domain.promo.PromoCode;
import com.example.lumoo.domain.promo.PromoCodeRepository;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
/** Runs the real Flyway migrations (V1..Vn) on H2 and checks the constraints they add. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // keep the MySQL-mode H2 from test properties
class MigrationConstraintsTest {
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired CartRepository carts;
    @Autowired ReviewRepository reviews;
    @Autowired JdbcTemplate jdbc;
    @Autowired PromoCodeRepository promos;
    private User user() {
        User u = new User();
        u.setEmail("buyer" + System.nanoTime() + "@test.com");
        u.setRole(Role.USER);
        return users.save(u);
    }
    private Product product() {
        Product p = new Product();
        p.setName("Cement");
        p.setPrice(100.0);
        return products.save(p);
    }
    @Test
    void migrationsAreApplied() {
        Integer applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE", Integer.class);
        assertTrue(applied >= 4);
    }
    @Test
    void duplicateCartLine_isRejected() {
        User u = user(); Product p = product();
        CartItem a = new CartItem(); a.setUser(u); a.setProduct(p); a.setQuantity(1);
        CartItem b = new CartItem(); b.setUser(u); b.setProduct(p); b.setQuantity(1);
        carts.saveAndFlush(a);
        assertThrows(DataIntegrityViolationException.class, () -> carts.saveAndFlush(b));
    }
    @Test
    void duplicateReview_isRejected() {
        User u = user(); Product p = product();
        Review a = new Review(); a.setUser(u); a.setProduct(p); a.setRating(5);
        Review b = new Review(); b.setUser(u); b.setProduct(p); b.setRating(4);
        reviews.saveAndFlush(a);
        assertThrows(DataIntegrityViolationException.class, () -> reviews.saveAndFlush(b));
    }
    @Test
    void promoUsageLimit_isEnforcedAtomically() {
        PromoCode p = new PromoCode();
        p.setCode("ONCE");
        p.setDiscountValue(10);
        p.setUsageLimit(1);
        p = promos.saveAndFlush(p);
        assertEquals(1, promos.incrementUsage(p.getId()));
        assertEquals(0, promos.incrementUsage(p.getId()));
        assertEquals(1, promos.findById(p.getId()).orElseThrow().getUsedCount());
    }
}
