package com.example.lumoo.domain.order;
import com.example.lumoo.domain.order.OrderItem;
import com.example.lumoo.domain.order.Order;
import com.example.lumoo.domain.product.Product;
import com.example.lumoo.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder(Order order);
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.order.user = :user AND oi.order.status = :status AND oi.product = :product")
    boolean existsByOrderUserAndOrderStatusAndProduct(@Param("user") User user, @Param("status") String status, @Param("product") Product product);
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi WHERE oi.order.user = :user AND oi.product = :product")
    boolean existsByOrderUserAndProduct(@Param("user") User user, @Param("product") Product product);
}
