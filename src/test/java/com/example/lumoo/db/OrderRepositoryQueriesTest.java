package com.example.lumoo.db;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.order.OrderItem;
import com.example.lumoo.domain.order.OrderItemRepository;
import com.example.lumoo.domain.order.OrderRepository;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.product.ProductRepository;
import com.example.lumoo.domain.user.Role;
import com.example.lumoo.domain.user.User;
import com.example.lumoo.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryQueriesTest {
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired EntityManager em;
    private User vendor() {
        User v = new User();
        v.setEmail("v" + System.nanoTime() + "@test.com");
        v.setRole(Role.VENDOR);
        return users.save(v);
    }
    private Order order(String status, double total, User vendor, int itemCount) {
        Order o = new Order();
        o.setStatus(status);
        o.setTotalAmount(total);
        o.setOrderDate(LocalDateTime.now());
        o = orders.save(o);
        Product p = new Product();
        p.setName("P");
        p.setPrice(1.0);
        p.setVendor(vendor);
        p = products.save(p);
        for (int i = 0; i < itemCount; i++) {
            OrderItem it = new OrderItem();
            it.setOrder(o);
            it.setProduct(p);
            it.setQuantity(1);
            items.save(it);
        }
        return o;
    }
    @Test
    void vendorRevenue_countsEachOrderOnce_andOnlyPaidOrLater() {
        User v = vendor();
        order("PAID", 100, v, 3);            // 3 items: the old JOIN summed this 3 times
        order("DELIVERED", 50, v, 1);
        order("PAYMENT_FAILED", 999, v, 1);  // never revenue
        order("CANCELLED", 999, v, 1);
        em.flush();
        assertEquals(150.0, orders.sumVendorRevenue(v.getId()), 0.001);
    }
    @Test
    void ordersWithNullCustomerName_areAnonymised() {
        Order old = order("DELIVERED", 10, vendor(), 1);
        old.setCreatedAt(LocalDateTime.now().minusYears(3));
        Order done = order("DELIVERED", 10, vendor(), 1);
        done.setCreatedAt(LocalDateTime.now().minusYears(3));
        done.setCustomerName("ANONYMISED");
        em.flush();
        var found = orders.findOrdersToAnonymise(LocalDateTime.now().minusYears(2));
        assertTrue(found.stream().anyMatch(o -> o.getId().equals(old.getId())));
        assertTrue(found.stream().noneMatch(o -> o.getId().equals(done.getId())));
    }
    @Test
    void staleUpdate_isRejectedByVersion() {
        Order o = order("AWAITING_PAYMENT", 10, vendor(), 1);
        em.flush();
        em.clear();
        Order copy1 = orders.findById(o.getId()).orElseThrow();
        em.detach(copy1);
        Order copy2 = orders.findById(o.getId()).orElseThrow();
        copy2.setStatus("PAID");
        orders.saveAndFlush(copy2);
        copy1.setStatus("PAYMENT_FAILED");
        assertThrows(OptimisticLockingFailureException.class, () -> orders.saveAndFlush(copy1));
    }
}
