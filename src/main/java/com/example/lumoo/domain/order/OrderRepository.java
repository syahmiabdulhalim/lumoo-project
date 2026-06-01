package com.example.lumoo.domain.order;
import com.example.lumoo.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByOrderDateDesc(User user);
    List<Order> findByStatusOrderByOrderDateDesc(String status);
    List<Order> findByCreatedAtBeforeAndCustomerNameNot(LocalDateTime before, String customerName);
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId")
    List<Order> findOrdersByVendorId(@Param("vendorId") Long vendorId);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.user = :user ORDER BY o.orderDate DESC")
    List<Order> findByUserWithItems(@Param("user") User user);
    @Query(value = "SELECT DISTINCT o FROM Order o JOIN o.items i WHERE o.user = :user",
           countQuery = "SELECT COUNT(DISTINCT o) FROM Order o WHERE o.user = :user")
    Page<Order> findByUserPaged(@Param("user") User user, Pageable pageable);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId")
    List<Order> findVendorOrdersWithItems(@Param("vendorId") Long vendorId);
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    double sumTotalRevenue();
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId")
    double sumVendorRevenue(@Param("vendorId") Long vendorId);
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId AND MONTH(o.createdAt) = MONTH(CURRENT_DATE) AND YEAR(o.createdAt) = YEAR(CURRENT_DATE)")
    double sumVendorMonthlySales(@Param("vendorId") Long vendorId);
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId")
    long countVendorOrders(@Param("vendorId") Long vendorId);
    @Query(value = "SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId",
           countQuery = "SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId")
    org.springframework.data.domain.Page<Order> findVendorOrdersPaged(@Param("vendorId") Long vendorId, org.springframework.data.domain.Pageable pageable);
    @Query("SELECT COALESCE(SUM(o.adminCommission), 0) FROM Order o")
    double sumTotalCommission();
    Optional<Order> findByPayoutTransferId(String payoutTransferId);
    List<Order> findByModempayPaymentId(String modempayPaymentId);
    List<Order> findByUserAndStatus(User user, String status);
    @Query("SELECT o FROM Order o WHERE o.status = 'AWAITING_PAYMENT' AND o.modempayPaymentId IS NOT NULL AND o.orderDate < :cutoff")
    List<Order> findStaleAwaitingPayment(@Param("cutoff") LocalDateTime cutoff);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.status = 'AWAITING_PROOF' AND o.orderDate < :cutoff")
    List<Order> findStaleAwaitingProof(@Param("cutoff") LocalDateTime cutoff);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.status = 'DELIVERED' AND o.payoutStatus = 'FAILED'")
    List<Order> findDeliveredWithFailedPayout();
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.status = 'SHIPPED' AND o.orderDate < :cutoff")
    List<Order> findStaleShipped(@Param("cutoff") LocalDateTime cutoff);
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") String status);
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderDate >= :since")
    double sumRevenueSince(@Param("since") LocalDateTime since);
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate >= :since")
    long countOrdersSince(@Param("since") LocalDateTime since);
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.payoutStatus = 'HELD' AND o.deliveredAt <= :cutoff")
    List<Order> findHeldPayoutsReadyForRelease(@Param("cutoff") LocalDateTime cutoff);
    @Query(value = "SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId AND o.status = :status",
           countQuery = "SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId AND o.status = :status")
    org.springframework.data.domain.Page<Order> findVendorOrdersPagedByStatus(@Param("vendorId") Long vendorId, @Param("status") String status, org.springframework.data.domain.Pageable pageable);
    @Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items i WHERE i.product IS NOT NULL AND i.product.vendor.id = :vendorId AND o.status = :status")
    long countVendorOrdersByStatus(@Param("vendorId") Long vendorId, @Param("status") String status);
}
